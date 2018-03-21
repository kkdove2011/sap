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
			throw new Exception("û��ѡ���κζ���");
		}
		SapFunction sapFunction = SapFunction.getInstance();
		// SapFunction.reloadXml();// �����ã�����xml
		// ���غ���
		sapFunction.loadFunction(commandId);
		// ����������
		checkTargets(targets, sapFunction.getFunctionNode());
		StringBuilder successInfo = new StringBuilder();
		StringBuilder failInfo = new StringBuilder();
		// ����targets��ȡ���ݺʹ��䣬�����쳣����������ʾ��Ϣ
		int counter=1;
		int successCounter=0;
		for (InterfaceAIFComponent target : targets) {
			try {
				// ��ȡtc����
				infoDialog.setText("��������... "+(counter++)+" of "+targetCnt);
				List<Map<String, String>> propMaps = initTCData(sapFunction.getImportTable(), (TCComponent) target, sapFunction.isBop());
				// ���͵�sap
				Map<String, JCoTable> rtnTableMap = sapFunction.sendToSap(propMaps);
				// �ж��Ƿ�ɹ�
				if (rtnTableMap.containsKey("RETURN")) {
					JCoTable rtnTable = rtnTableMap.get("RETURN");
					if ("S".equals(rtnTable.getString("TYPE"))) {
						successInfo.append("---------------------------------------------------------\n");
						successInfo.append("���ͳɹ���").append(target.toString()).append("\n");
						successCounter++;
					} else {
						failInfo.append("---------------------------------------------------------\n");
						failInfo.append("����ʧ�ܣ�").append(target.toString()).append("\n������Ϣ��").append(rtnTable.getString("MESSAGE")).append("\n");
					}
				} else {
					failInfo.append("---------------------------------------------------------\n");
					failInfo.append("����ʧ�ܣ�").append(target.toString()).append("\n�޷���ȡ���ؽ���ı�RETURN\n");
				}
			} catch (Exception e) {
				failInfo.append("---------------------------------------------------------\n");
				failInfo.append("����ʧ�ܣ�").append(target.toString()).append("\n").append(e.getMessage()).append("\n");
				e.printStackTrace();
			}
		}
		successInfo.append("---------------------------------------------------------\n");
		infoDialog.disposeDialog();
		KMessageBox.post("���ͽ�����ɹ�"+successCounter+"����ʧ��"+(targetCnt-successCounter)+"��\n" + failInfo.toString()+successInfo.toString(), "��Ϣ");
	}

	/**
	 * ������������ֱ���׳��쳣
	 * 
	 * @param targets
	 * @param functionNode
	 * @throws Exception
	 */
	private void checkTargets(InterfaceAIFComponent[] targets, XmlBean functionNode) throws Exception {
		List<XmlBean> childNodes = functionNode.getChildNodes("FieldConfig");
		if (childNodes.size() == 0)
			throw new Exception("Function�ڵ���û���ҵ�FieldConfig�ڵ㣬����XML�ļ���" + SapFunction.XMLPATH);
		if (childNodes.size() > 1)
			throw new Exception("Function�ڵ����ҵ����FieldConfig�ڵ㣬����XML�ļ���" + SapFunction.XMLPATH);
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
			throw new Exception("ѡ�е����ʹ���\n��������ͣ�" + xType + "\n������Ķ���" + errInfo.toString());
		}
	}

	/**
	 * ��tc��ȡ����
	 * 
	 * @param dataTable
	 *            SAP���װ��
	 * @param target
	 *            Ŀ�����
	 * @param isBop
	 *            true������滮����false���ṹ������
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
			throw new Exception("��ѡ��BOMLine���߶���汾���в���");
		}
		// ��ȡ����汾��BomLine
		String tableSource = dataTable.getTableSource();// Item,BomLine,Self
		// �ж��Ƿ���Ҫ����bom
		boolean isCycle = false;
		if ("bomline".equalsIgnoreCase(tableSource)) {
			isCycle = true;
		}
		List<Map<String, String>> propMaps = new ArrayList<>();
		// ������Ե�propMaps
		addToDataMap(dataTable, isCycle, sourceRev, sourceBomLine, propMaps);
		// �ر�bomWindow
		if (bomWindow != null)
			bomWindow.close();
		return propMaps;
	}

	/**
	 * ��ѯ���Բ�����propMaps
	 * 
	 * @param dataTable
	 *            SAP��
	 * @param isCycle
	 *            �Ƿ�Ҫ����BOM
	 * @param sourceRev
	 * @param sourceBomLine
	 * @param propMaps
	 * @throws Exception
	 */
	private void addToDataMap(SapDataTable dataTable, boolean isCycle, TCComponentItemRevision sourceRev, TCComponentBOMLine sourceBomLine, List<Map<String, String>> propMaps) throws Exception {
		if (isCycle) {// ��Ҫѭ��bom���д���
			if (sourceBomLine.hasChildren()) {
				cycleBom(dataTable, isCycle, sourceRev, sourceBomLine, propMaps);
			} else {
				throw new Exception("���󲻴���BOM�ṹ��" + sourceRev);
			}
		} else {
			propMaps.add(getPropMap(dataTable, isCycle, sourceRev, sourceBomLine));
		}
	}

	/**
	 * ���bomʱ������bom�ṹ��������в���Ϊtarget����
	 * 
	 * @param dataTable
	 *            SAP���װ�����
	 * @param isCycle
	 *            �Ƿ��ڱ���bom
	 * @param rev
	 *            ����Ŀ��汾
	 * @param bomline
	 *            ����Ŀ��bomline
	 * @param propMaps
	 *            �������ݵļ���
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
	 * ��ȡ��������Map
	 * 
	 * @param dataTable
	 *            Ҫ��д��SapTable
	 * @param isCycle
	 *            �Ƿ��ڱ���bom
	 * @param rev
	 *            ��ǰ����汾
	 * @param bomline
	 *            ��ǰ����汾��Ӧ��bomline
	 * @return ������ǰ���������������ݵļ���
	 * @throws Exception
	 */
	private Map<String, String> getPropMap(SapDataTable dataTable, boolean isCycle, TCComponentItemRevision rev, TCComponentBOMLine bomline) throws Exception {
		String tcFieldName;
		String sapFieldName;
		String dataStr;
		// ����FieldConfigд������
		List<SapDataField> sapDataFields = dataTable.getSapDataFields();
		Map<String, String> propMap = new HashMap<>();
		for (SapDataField sapDataField : sapDataFields) {
			TCComponent dataComp = getDataComp(sapDataField, isCycle, rev, bomline);
			tcFieldName = sapDataField.getTcFieldName();
			sapFieldName = sapDataField.getSapFieldName();
			if (!"".equals(tcFieldName)) {
				if (dataComp == null) { // ����ĸ��в�����
					dataStr = "";
				} else {
					dataStr = getRealValue(dataComp, tcFieldName);
				}
			} else {
				throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "��û������PlmField");
			}
			// ���sapFieldName
			if ("".equals(sapFieldName)) {
				throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "��û������SapField");
			}
			// ��ȡ�����tc����ֵ
			dataStr = getCheckedData(sapDataField, dataStr);
			propMap.put(sapFieldName, dataStr);
			System.out.println("|" + tcFieldName + "=" + dataStr + "\t|" + sapDataField.getText() + "|");
		}
		System.out.println("-----------------------------------------------------");
		return propMap;
	}

	/**
	 * ���͸�ʽ��tc����ֵ
	 * 
	 * @param sapDataField
	 *            XML��FieldConfig�ڵ��װ��
	 * @param dataStr
	 *            tc����ֵ
	 * @return ����������ʽ��֮�������ֵ
	 * @throws Exception
	 */
	private String getCheckedData(SapDataField sapDataField, String dataStr) throws Exception {
		String regex = sapDataField.getRegex();
		String defaultValue = sapDataField.getDefaultValue();
		String formatter = sapDataField.getFormatter();
		boolean isEmpty = sapDataField.isEmpty();
		String dataType = sapDataField.getSapDataType();
		String dataLength = sapDataField.getSapDataLength();
		// �Ƿ�Ϊ��
		if (!isEmpty && "".equals(dataStr)) {
			throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����Ӧtc����" + sapDataField.getTcFieldName() + "����Ϊ��");
		}
		// Ĭ��ֵ
		if ("".equals(dataStr) && !"".equals(defaultValue)) {
			dataStr = defaultValue;
		}
		// Format
		if (!"".equals(formatter)) {
			dataStr = String.format(formatter, dataStr);
		}
		// ������ʽ
		if (!"".equals(regex)) {
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(dataStr);
			if (!matcher.find()) {
				throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����Ӧtc����" + sapDataField.getTcFieldName() + "=" + dataStr + "��ʽ����\n��ȷ��ʽ��" + regex);
			}
		}
		// ���ݳ���
		if (!"".endsWith(dataLength)) {
			try {
				int length = Integer.parseInt(dataLength);
				if (dataStr.length() > length) {
					throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����Ӧtc����" + sapDataField.getTcFieldName() + "=" + dataStr + "���ݳ��ȹ���\n��󳤶ȣ�" + length);
				}
			} catch (Exception e) {
				throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "�����ݳ�������Length��ֵ����������" + dataLength);
			}
		}
		// ��������
		if (!"".equals(dataType)) {
			// TODO
		}
		return dataStr;
	}

	/**
	 * ��ȡ����������ʵֵ
	 * 
	 * @param comp
	 * @param propName xml�ļ������õ�PlmField��������
	 * @return
	 * @throws Exception
	 */
	private String getRealValue(TCComponent comp, String propName) throws Exception {
		TCProperty property = comp.getTCProperty(propName);
		String propVal;
		if (property == null) {
			System.out.println("���Բ�����");
			throw new Exception("�������Բ�����\n����" + comp + "\n�������ͣ�" + comp.getType() + "\n�������ƣ�" + propName);
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
	 * ��ȡ�����ж�Ӧ��tc����
	 * 
	 * @param sapDataField
	 *            ��װSAP-TC����ӳ��ڵ�FieldConfig�ķ�װ�����
	 * @param isCycle
	 *            �Ƿ��ڱ���bom
	 * @param sourceRev
	 *            ��ǰ����汾
	 * @param sourceBomLine
	 *            ��ǰ����bomline
	 * @return ����ӳ��ڵ�õ����������ڶ���
	 * @throws Exception
	 */
	private static TCComponent getDataComp(SapDataField sapDataField, boolean isCycle, TCComponentItemRevision sourceRev, TCComponentBOMLine sourceBomLine) throws Exception {
		TCComponent dataComp = null;
		// ����bom
		if (isCycle) {
			// bomlineҪ�������ݣ�sourceRev��sourceBomLine�滻Ϊ����
			if ("parent".equalsIgnoreCase(sapDataField.getFieldType())) {
				TCComponentBOMLine parentLine = sourceBomLine.parent();
				if (parentLine != null) {
					sourceRev = parentLine.getItemRevision();
					sourceBomLine = parentLine;
				} else {
					return null;
				}
			}
			// ����Ƿ�Ҫ���ϵ����ϵ�Ӱ汾Revȡ��
			if (!"".equals(sapDataField.getRelation())) {
				if ("rev".equalsIgnoreCase(sapDataField.getRelation())) {
					return sourceRev;
				}
				TCComponent[] relatedComps = sourceRev.getRelatedComponents(sapDataField.getRelation());
				if (relatedComps.length == 0) {
					throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����ȡ��ϵ����" + sapDataField.getRelation() + "ʱ����û���ҵ���ϵ����\n");
				}
				if (!"".equals(sapDataField.getForm())) {
					for (TCComponent relatedComp : relatedComps) {
						if (sapDataField.getForm().equals(relatedComp.getType())) {
							dataComp = relatedComp;
							break;
						}
					}
					if (dataComp == null) {
						throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����ȡ��ϵ����" + sapDataField.getRelation() + "ʱ����û���ҵ�" + sapDataField.getForm() + "����\n");
					}
				}
			}
			if (dataComp != null) {
				return dataComp;
			}
			return sourceBomLine;
		} else {
			// �Ǳ���bom
			if ("bomline".equalsIgnoreCase(sapDataField.getFieldType())) {
				return sourceBomLine;
			}
			// ����Ƿ�Ҫ���ϵ����ϵ�Ӱ汾Revȡ��
			if (!"".equals(sapDataField.getRelation())) {
				TCComponent[] relatedComps = sourceRev.getRelatedComponents(sapDataField.getRelation());
				if (relatedComps.length == 0) {
					throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����ȡ��ϵ����" + sapDataField.getRelation() + "ʱ����û���ҵ���ϵ����\n");
				}
				if (!"".equals(sapDataField.getForm())) {
					for (TCComponent relatedComp : relatedComps) {
						if (sapDataField.getForm().equals(relatedComp.getType())) {
							dataComp = relatedComp;
							break;
						}
					}
					if (dataComp == null) {
						throw new Exception("SAP�ֶΡ�" + sapDataField.getText() + "����ȡ��ϵ����" + sapDataField.getRelation() + "ʱ����û���ҵ�" + sapDataField.getForm() + "����\n");
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
