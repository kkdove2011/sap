package com.connor.sap.send;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.connor.sap.util.SapDataField;
import com.connor.sap.util.SapDataTable;
import com.connor.sap.util.SapFunction;
import com.connor.sap.xml.XmlBean;
import com.sap.conn.jco.JCoTable;
import com.teamcenter.rac.aif.AbstractAIFOperation;
import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.aif.kernel.AIFComponentContext;
import com.teamcenter.rac.aif.kernel.InterfaceAIFComponent;
import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentBOMLine;
import com.teamcenter.rac.kernel.TCComponentBOMWindow;
import com.teamcenter.rac.kernel.TCComponentBOMWindowType;
import com.teamcenter.rac.kernel.TCComponentBOPWindowType;
import com.teamcenter.rac.kernel.TCComponentItemRevision;
import com.teamcenter.rac.kernel.TCProperty;
import com.teamcenter.rac.kernel.TCSession;

public class ToSapOperation extends AbstractAIFOperation {
	private AbstractAIFUIApplication app;
	private String commandId;
	private InterfaceAIFComponent[] targets;
	private KInfoDialog infoDialog;

	public ToSapOperation(AbstractAIFUIApplication app, String commandId, KInfoDialog infoDialog) {
		this.app = app;
		this.commandId = commandId;
		this.targets = app.getTargetComponents();
		this.infoDialog=infoDialog;
	}

	@Override
	public void executeOperation() throws Exception {
		int targetCnt=targets.length;
		if (targetCnt == 0) {
			throw new Exception("没有选择任何对象");
		}
		SapFunction sapFunction = SapFunction.getInstance();
		// SapFunction.reloadXml();// 测试用，更新xml
		// 加载函数
		sapFunction.loadFunction(commandId);
		// 检查对象类型
		checkTargets(targets, sapFunction.getFunctionNode());
		StringBuilder successInfo = new StringBuilder();
		StringBuilder failInfo = new StringBuilder();
		// 遍历targets获取数据和传输，进行异常处理并处理提示信息
		int counter=1;
		int successCounter=0;
		for (InterfaceAIFComponent target : targets) {
			try {
				// 获取tc数据
				infoDialog.setText("传送数据... "+(counter++)+" of "+targetCnt);
				List<Map<String, String>> propMaps = initTCData(sapFunction.getImportTable(), (TCComponent) target, sapFunction.isBop());
				// 发送到sap
				Map<String, JCoTable> rtnTableMap = sapFunction.sendToSap(propMaps);
				// 判断是否成功
				if (rtnTableMap.containsKey("RETURN")) {
					JCoTable rtnTable = rtnTableMap.get("RETURN");
					if ("S".equals(rtnTable.getString("TYPE"))) {
						successInfo.append("---------------------------------------------------------\n");
						successInfo.append("传送成功：").append(target.toString()).append("\n");
						successCounter++;
					} else {
						failInfo.append("---------------------------------------------------------\n");
						failInfo.append("传送失败：").append(target.toString()).append("\n返回信息：").append(rtnTable.getString("MESSAGE")).append("\n");
					}
				} else {
					failInfo.append("---------------------------------------------------------\n");
					failInfo.append("传送失败：").append(target.toString()).append("\n无法获取返回结果的表：RETURN\n");
				}
			} catch (Exception e) {
				failInfo.append("---------------------------------------------------------\n");
				failInfo.append("传送失败：").append(target.toString()).append("\n").append(e.getMessage()).append("\n");
				e.printStackTrace();
			}
		}
		successInfo.append("---------------------------------------------------------\n");
		infoDialog.disposeDialog();
		KMessageBox.post("传送结果：成功"+successCounter+"个，失败"+(targetCnt-successCounter)+"个\n" + failInfo.toString()+successInfo.toString(), "信息");
	}

	/**
	 * 检查对象，有问题直接抛出异常
	 * 
	 * @param targets
	 * @param functionNode
	 * @throws Exception
	 */
	private void checkTargets(InterfaceAIFComponent[] targets, XmlBean functionNode) throws Exception {
		List<XmlBean> childNodes = functionNode.getChildNodes("FieldConfig");
		if (childNodes.size() == 0)
			throw new Exception("Function节点下没有找到FieldConfig节点，请检查XML文件：" + SapFunction.XMLPATH);
		if (childNodes.size() > 1)
			throw new Exception("Function节点下找到多个FieldConfig节点，请检查XML文件：" + SapFunction.XMLPATH);
		String xType = childNodes.get(0).getNodeValue("PlmInfoType");
		StringBuilder errInfo = new StringBuilder();
		for (InterfaceAIFComponent target : targets) {
			String type = target.getType();
			if ("BOMLine".equals(type)) {
				type = ((TCComponentBOMLine) target).getItemRevision().getType();
			}
			if (!xType.equals(type)) {
				errInfo.append(target.toString()).append("(").append(type).append("),");
			}
		}
		if (errInfo.length() > 0) {
			errInfo.setLength(errInfo.length() - 1);
			throw new Exception("选中的类型错误\n需求的类型：" + xType + "\n有问题的对象：" + errInfo.toString());
		}
	}

	/**
	 * 从tc获取数据
	 * 
	 * @param dataTable
	 *            SAP表封装类
	 * @param target
	 *            目标对象
	 * @param isBop
	 *            true：零件规划器；false：结构管理器
	 * @return
	 * @throws Exception
	 */
	private List<Map<String, String>> initTCData(SapDataTable dataTable, TCComponent target, boolean isBop) throws Exception {
		TCComponentBOMWindow bomWindow = null;
		TCComponentItemRevision sourceRev;
		TCComponentBOMLine sourceBomLine;
		String type = target.getType();
		if ("BOMLine".equals(type)) {
			sourceBomLine = (TCComponentBOMLine) target;
			sourceRev = sourceBomLine.getItemRevision();
		} else if (target instanceof TCComponentItemRevision) {
			sourceRev = (TCComponentItemRevision) target;
			TCSession session = (TCSession) app.getSession();
			if (isBop) {
				TCComponentBOPWindowType bopWindowType = (TCComponentBOPWindowType) session.getTypeComponent("BOPWindow");
				bomWindow = bopWindowType.create(null);
			} else {
				TCComponentBOMWindowType bomWindowType = (TCComponentBOMWindowType) session.getTypeComponent("BOMWindow");
				bomWindow = bomWindowType.create(null);
			}
			sourceBomLine = bomWindow.setWindowTopLine(sourceRev.getItem(), sourceRev, null, null);
		} else {
			throw new Exception("请选择BOMLine或者对象版本进行操作");
		}
		// 获取对象版本和BomLine
		String tableSource = dataTable.getTableSource();// Item,BomLine,Self
		// 判断是否需要遍历bom
		boolean isCycle = false;
		if ("bomline".equalsIgnoreCase(tableSource)) {
			isCycle = true;
		}
		List<Map<String, String>> propMaps = new ArrayList<>();
		// 添加属性到propMaps
		addToDataMap(dataTable, isCycle, sourceRev, sourceBomLine, propMaps);
		// 关闭bomWindow
		if (bomWindow != null)
			bomWindow.close();
		return propMaps;
	}

	/**
	 * 查询属性并加入propMaps
	 * 
	 * @param dataTable
	 *            SAP表
	 * @param isCycle
	 *            是否要遍历BOM
	 * @param sourceRev
	 * @param sourceBomLine
	 * @param propMaps
	 * @throws Exception
	 */
	private void addToDataMap(SapDataTable dataTable, boolean isCycle, TCComponentItemRevision sourceRev, TCComponentBOMLine sourceBomLine, List<Map<String, String>> propMaps) throws Exception {
		if (isCycle) {// 需要循环bom进行传递
			if (sourceBomLine.hasChildren()) {
				cycleBom(dataTable, isCycle, sourceRev, sourceBomLine, propMaps);
			} else {
				throw new Exception("对象不存在BOM结构：" + sourceRev);
			}
		} else {
			propMaps.add(getPropMap(dataTable, isCycle, sourceRev, sourceBomLine));
		}
	}

	/**
	 * 输出bom时，遍历bom结构输出，顶行不作为target数据
	 * 
	 * @param dataTable
	 *            SAP表封装类对象
	 * @param isCycle
	 *            是否在遍历bom
	 * @param rev
	 *            导出目标版本
	 * @param bomline
	 *            导出目标bomline
	 * @param propMaps
	 *            储存数据的集合
	 * @throws Exception
	 */
	private void cycleBom(SapDataTable dataTable, boolean isCycle, TCComponentItemRevision rev, TCComponentBOMLine bomline, List<Map<String, String>> propMaps) throws Exception {
		AIFComponentContext[] childContexts = bomline.getChildren();
		if (childContexts.length > 0) {
			for (AIFComponentContext childContext : childContexts) {
				TCComponentBOMLine childLine = (TCComponentBOMLine) childContext.getComponent();
				propMaps.add(getPropMap(dataTable, isCycle, childLine.getItemRevision(), childLine));
				cycleBom(dataTable, isCycle, childLine.getItemRevision(), childLine, propMaps);
			}
		}
	}

	/**
	 * 获取对象属性Map
	 * 
	 * @param dataTable
	 *            要填写的SapTable
	 * @param isCycle
	 *            是否在遍历bom
	 * @param rev
	 *            当前对象版本
	 * @param bomline
	 *            当前对象版本对应的bomline
	 * @return 包含当前导出对象属性数据的集合
	 * @throws Exception
	 */
	private Map<String, String> getPropMap(SapDataTable dataTable, boolean isCycle, TCComponentItemRevision rev, TCComponentBOMLine bomline) throws Exception {
		String tcFieldName;
		String sapFieldName;
		String dataStr;
		// 遍历FieldConfig写入数据
		List<SapDataField> sapDataFields = dataTable.getSapDataFields();
		Map<String, String> propMap = new HashMap<>();
		for (SapDataField sapDataField : sapDataFields) {
			TCComponent dataComp = getDataComp(sapDataField, isCycle, rev, bomline);
			tcFieldName = sapDataField.getTcFieldName();
			sapFieldName = sapDataField.getSapFieldName();
			if (!"".equals(tcFieldName)) {
				if (dataComp == null) { // 顶层的父行不存在
					dataStr = "";
				} else {
					dataStr = getRealValue(dataComp, tcFieldName);
				}
			} else {
				throw new Exception("SAP字段“" + sapDataField.getText() + "”没有配置PlmField");
			}
			// 检查sapFieldName
			if ("".equals(sapFieldName)) {
				throw new Exception("SAP字段“" + sapDataField.getText() + "”没有配置SapField");
			}
			// 获取检查后的tc属性值
			dataStr = getCheckedData(sapDataField, dataStr);
			propMap.put(sapFieldName, dataStr);
			System.out.println("|" + tcFieldName + "=" + dataStr + "\t|" + sapDataField.getText() + "|");
		}
		System.out.println("-----------------------------------------------------");
		return propMap;
	}

	/**
	 * 检查和格式化tc属性值
	 * 
	 * @param sapDataField
	 *            XML中FieldConfig节点封装类
	 * @param dataStr
	 *            tc属性值
	 * @return 根据条件格式化之后的属性值
	 * @throws Exception
	 */
	private String getCheckedData(SapDataField sapDataField, String dataStr) throws Exception {
		String regex = sapDataField.getRegex();
		String defaultValue = sapDataField.getDefaultValue();
		String formatter = sapDataField.getFormatter();
		boolean isEmpty = sapDataField.isEmpty();
		String dataType = sapDataField.getSapDataType();
		String dataLength = sapDataField.getSapDataLength();
		// 是否为空
		if (!isEmpty && "".equals(dataStr)) {
			throw new Exception("SAP字段“" + sapDataField.getText() + "”对应tc属性" + sapDataField.getTcFieldName() + "不可为空");
		}
		// 默认值
		if ("".equals(dataStr) && !"".equals(defaultValue)) {
			dataStr = defaultValue;
		}
		// Format
		if (!"".equals(formatter)) {
			dataStr = String.format(formatter, dataStr);
		}
		// 正则表达式
		if (!"".equals(regex)) {
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(dataStr);
			if (!matcher.find()) {
				throw new Exception("SAP字段“" + sapDataField.getText() + "”对应tc属性" + sapDataField.getTcFieldName() + "=" + dataStr + "格式错误\n正确格式：" + regex);
			}
		}
		// 数据长度
		if (!"".endsWith(dataLength)) {
			try {
				int length = Integer.parseInt(dataLength);
				if (dataStr.length() > length) {
					throw new Exception("SAP字段“" + sapDataField.getText() + "”对应tc属性" + sapDataField.getTcFieldName() + "=" + dataStr + "数据长度过长\n最大长度：" + length);
				}
			} catch (Exception e) {
				throw new Exception("SAP字段“" + sapDataField.getText() + "”数据长度属性Length的值不是整数：" + dataLength);
			}
		}
		// 数据类型
		if (!"".equals(dataType)) {
			// TODO
		}
		return dataStr;
	}

	/**
	 * 获取对象属性真实值
	 * 
	 * @param comp
	 * @param propName xml文件中配置的PlmField属性内容
	 * @return
	 * @throws Exception
	 */
	private String getRealValue(TCComponent comp, String propName) throws Exception {
		TCProperty property = comp.getTCProperty(propName);
		String propVal;
		if (property == null) {
			System.out.println("属性不存在");
			throw new Exception("对象属性不存在\n对象：" + comp + "\n对象类型：" + comp.getType() + "\n属性名称：" + propName);
			// return "";
		}
		int propertyType = property.getPropertyType();
		switch (propertyType) {
		case TCProperty.PROP_string:
			propVal = property.getStringValue();
			break;
		case TCProperty.PROP_date:
			propVal = property.getDateValue().toString();
			break;
		case TCProperty.PROP_int:
			propVal = String.valueOf(property.getIntValue());
			break;
		case TCProperty.PROP_double:
			propVal = String.valueOf(property.getDoubleValue());
			break;
		case TCProperty.PROP_short:
			propVal = String.valueOf(property.getShortValue());
			break;
		default:
			propVal = property.getDisplayValue();
			break;
		}
		if (propVal == null) {
			propVal = "";
		}
		return propVal;
	}

	/**
	 * 获取属性行对应的tc对象
	 * 
	 * @param sapDataField
	 *            封装SAP-TC属性映射节点FieldConfig的封装类对象
	 * @param isCycle
	 *            是否在遍历bom
	 * @param sourceRev
	 *            当前对象版本
	 * @param sourceBomLine
	 *            当前对象bomline
	 * @return 根据映射节点得到的属性所在对象
	 * @throws Exception
	 */
	private static TCComponent getDataComp(SapDataField sapDataField, boolean isCycle, TCComponentItemRevision sourceRev, TCComponentBOMLine sourceBomLine) throws Exception {
		TCComponent dataComp = null;
		// 遍历bom
		if (isCycle) {
			// bomline要求父行数据，sourceRev和sourceBomLine替换为父行
			if ("parent".equalsIgnoreCase(sapDataField.getFieldType())) {
				TCComponentBOMLine parentLine = sourceBomLine.parent();
				if (parentLine != null) {
					sourceRev = parentLine.getItemRevision();
					sourceBomLine = parentLine;
				} else {
					return null;
				}
			}
			// 检查是否要求关系，关系从版本Rev取得
			if (!"".equals(sapDataField.getRelation())) {
				if ("rev".equalsIgnoreCase(sapDataField.getRelation())) {
					return sourceRev;
				}
				TCComponent[] relatedComps = sourceRev.getRelatedComponents(sapDataField.getRelation());
				if (relatedComps.length == 0) {
					throw new Exception("SAP字段“" + sapDataField.getText() + "”获取关系属性" + sapDataField.getRelation() + "时出错，没有找到关系对象\n");
				}
				if (!"".equals(sapDataField.getForm())) {
					for (TCComponent relatedComp : relatedComps) {
						if (sapDataField.getForm().equals(relatedComp.getType())) {
							dataComp = relatedComp;
							break;
						}
					}
					if (dataComp == null) {
						throw new Exception("SAP字段“" + sapDataField.getText() + "”获取关系属性" + sapDataField.getRelation() + "时出错，没有找到" + sapDataField.getForm() + "对象\n");
					}
				}
			}
			if (dataComp != null) {
				return dataComp;
			}
			return sourceBomLine;
		} else {
			// 非遍历bom
			if ("bomline".equalsIgnoreCase(sapDataField.getFieldType())) {
				return sourceBomLine;
			}
			// 检查是否要求关系，关系从版本Rev取得
			if (!"".equals(sapDataField.getRelation())) {
				TCComponent[] relatedComps = sourceRev.getRelatedComponents(sapDataField.getRelation());
				if (relatedComps.length == 0) {
					throw new Exception("SAP字段“" + sapDataField.getText() + "”获取关系属性" + sapDataField.getRelation() + "时出错，没有找到关系对象\n");
				}
				if (!"".equals(sapDataField.getForm())) {
					for (TCComponent relatedComp : relatedComps) {
						if (sapDataField.getForm().equals(relatedComp.getType())) {
							dataComp = relatedComp;
							break;
						}
					}
					if (dataComp == null) {
						throw new Exception("SAP字段“" + sapDataField.getText() + "”获取关系属性" + sapDataField.getRelation() + "时出错，没有找到" + sapDataField.getForm() + "对象\n");
					}
				}
			}
			if (dataComp != null) {
				return dataComp;
			}
			return sourceRev;
		}
	}

}
