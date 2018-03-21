package com.connor.sap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.connor.sap.util.SapConnector;
import com.connor.sap.util.SapSender;
import com.connor.sap.xml.XmlBean;
import com.connor.sap.xml.XmlHandler;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoTable;

public class SapFunction {
	/** XML文档根节点 */
	private static XmlBean rootNode;
	/** 单例 */
	private static SapFunction instance;
	/** 同步辅助对象 */
	private static final Object syncLock = new Object();
	/** XML文件路径 */
	public static final String XMLPATH = "G:\\SAP\\SAPIntegrationConfigRev.xml";
	/** 连接SAP类 */
	private static SapConnector connector;
	/** 目标函数节点 */
	private XmlBean functionNode;
	/** 目标函数名 */
	private String functionName;
	/** 需要导入SAP的表名 */
	private String importTableName;
	/** 需要导入的表 */
	private SapDataTable importTable;
	/** 需要导出的表（只用到表名属性） */
	private List<SapDataTable> outTables = new ArrayList<>();
	/** 是否工艺BOM */
	private boolean isBOP = false;

	public XmlBean getFunctionNode() {
		return functionNode;
	}

	public SapDataTable getImportTable() {
		return importTable;
	}

	public boolean isBop() {
		return isBOP;
	}

	private SapFunction() {
	}

	public static SapFunction getInstance() throws Exception {
		if (instance == null) {
			synchronized (syncLock) {
				if (instance == null) {
					instance = new SapFunction();
					XmlHandler xmlHandler = new XmlHandler();
					// 载入xml
					if (!xmlHandler.load(XMLPATH)) {
						throw new Exception("加载xml文件失败，路径：" + XMLPATH);
					}
					// 解析xml
					rootNode = xmlHandler.parseXml();
					// 获取连接
					List<XmlBean> childNodes = rootNode.getChildNodes("Organize");
					if (childNodes.size() == 0)
						throw new Exception("根节点下没有找到Organize节点，请检查XML文件：" + XMLPATH);
					XmlBean e = childNodes.get(0);
					connector = new SapConnector();
					connector.setClient(e.getNodeValue("Client"));
					connector.setHost(e.getNodeValue("AppServerHost"));
					connector.setJcoPeakLimit(e.getNodeValue("JCO_PEAK_LIMIT"));
					connector.setJcoPoolCapacity(e.getNodeValue("PoolSize"));
					connector.setLanguage(e.getNodeValue("Language"));
					connector.setPassword(e.getNodeValue("Password"));
					connector.setSysNo(e.getNodeValue("SystemNumber"));
					connector.setUser(e.getNodeValue("User"));
					connector.setDestinationName("ABAP_AS" + e.getNodeValue("Name"));
				}
			}
		}
		return instance;
	}
	/**
	 * 测试用，重载xml，使用后需要重新loadFunction
	 * @throws Exception
	 */
	public static void reloadXml() throws Exception {
		XmlHandler xmlHandler = new XmlHandler();
		// 载入xml
		if (!xmlHandler.load(XMLPATH)) {
			throw new Exception("加载xml文件失败，路径：" + XMLPATH);
		}
		// 解析xml
		rootNode = xmlHandler.parseXml();
		// 获取连接
		List<XmlBean> childNodes = rootNode.getChildNodes("Organize");
		if (childNodes.size() == 0)
			throw new Exception("根节点下没有找到Organize节点，请检查XML文件：" + XMLPATH);
		XmlBean e = childNodes.get(0);
		connector = new SapConnector();
		connector.setClient(e.getNodeValue("Client"));
		connector.setHost(e.getNodeValue("AppServerHost"));
		connector.setJcoPeakLimit(e.getNodeValue("JCO_PEAK_LIMIT"));
		connector.setJcoPoolCapacity(e.getNodeValue("PoolSize"));
		connector.setLanguage(e.getNodeValue("Language"));
		connector.setPassword(e.getNodeValue("Password"));
		connector.setSysNo(e.getNodeValue("SystemNumber"));
		connector.setUser(e.getNodeValue("User"));
		connector.setDestinationName("ABAP_AS" + e.getNodeValue("Name"));
	}

	/**
	 * 加载函数，读入目标函数的节点以及子节点属性
	 * @param commandId	TC菜单的CommandId
	 * @throws Exception
	 */
	public void loadFunction(String commandId) throws Exception {
		String nameVal;
		if (commandId.endsWith("ItemToSap")) {
			nameVal = "PartTOSap";
			isBOP = false;
		} else if (commandId.endsWith("ProcessToSap")) {
			nameVal = "ZPLM_MAINTAIN_ROUTING";
			isBOP = true;
		} else if (commandId.endsWith("BomToSap")) {
			nameVal = "BomTOSap";
			isBOP = false;
		} else {
			throw new Exception("没有找到当前命令对应的函数：CommandId=" + commandId);
		}
		List<XmlBean> childNodes = rootNode.getChildNodes("Function");
		for (XmlBean xmlBean : childNodes) {
			if (nameVal.equals(xmlBean.getNodeValue("Name"))) {
				this.functionNode = xmlBean;
				this.functionName = functionNode.getNodeValue("FunctionName");
				this.importTableName = functionNode.getNodeValue("ImportTable");
				initTablesAndFields();
				return;
			}
		}
	}
	
	/**
	 * 发送数据到SAP
	 * @param propMaps 保存需要导入SAP表的数据
	 * @return	Function下定义的IsReturn=true的JCoTable的集合
	 * @throws Exception
	 */
	public Map<String, JCoTable> sendToSap(List<Map<String, String>> propMaps) throws Exception {
		JCoDestination dest = connSap();
		List<String> outTableNames = new ArrayList<>();
		if (outTables.size() > 0) {
			for (SapDataTable outTable : outTables) {
				outTableNames.add(outTable.getTableName());
			}
		}
		Map<String, JCoTable> rtnTableMap = SapSender.sendData(dest, this.functionName, this.importTableName, propMaps, outTableNames);
		return rtnTableMap;
	}

	/**
	 * 初始化函数属性表单和字段信息
	 * @throws Exception
	 */
	private void initTablesAndFields() throws Exception {
		importTable = null;
		outTables.clear();
		List<XmlBean> childNodes = functionNode.getChildNodes("Table");
		if (childNodes.size() == 0)
			throw new Exception("Function节点下没有找到Table节点，请检查XML文件：" + XMLPATH);
		for (XmlBean tableNode : childNodes) {
			SapDataTable sapDataTable = new SapDataTable(tableNode);
			if (sapDataTable.isReturn()) {
				outTables.add(sapDataTable);
			}
			if (this.importTableName.equals(sapDataTable.getTableName())) {
				importTable = sapDataTable;
			}
		}
		if(importTable==null) {
			throw new Exception("Function节点下没有找到需要的ImportTable\nFunctionName:"+this.functionName+"\nImportTable:"+this.importTableName);
		}
	}

	/**
	 * 连接SAP
	 * @return
	 * @throws Exception
	 */
	private static JCoDestination connSap() throws Exception {
		// 连接SAP
		JCoDestination dest = connector.getSAPDestination();
		if (dest == null)
			throw new Exception("连接SAP失败");
		return dest;
	}

}
