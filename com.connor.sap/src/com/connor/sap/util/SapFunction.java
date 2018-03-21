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
	/** XML�ĵ����ڵ� */
	private static XmlBean rootNode;
	/** ���� */
	private static SapFunction instance;
	/** ͬ���������� */
	private static final Object syncLock = new Object();
	/** XML�ļ�·�� */
	public static final String XMLPATH = "G:\\SAP\\SAPIntegrationConfigRev.xml";
	/** ����SAP�� */
	private static SapConnector connector;
	/** Ŀ�꺯���ڵ� */
	private XmlBean functionNode;
	/** Ŀ�꺯���� */
	private String functionName;
	/** ��Ҫ����SAP�ı��� */
	private String importTableName;
	/** ��Ҫ����ı� */
	private SapDataTable importTable;
	/** ��Ҫ�����ı�ֻ�õ��������ԣ� */
	private List<SapDataTable> outTables = new ArrayList<>();
	/** �Ƿ���BOM */
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
					// ����xml
					if (!xmlHandler.load(XMLPATH)) {
						throw new Exception("����xml�ļ�ʧ�ܣ�·����" + XMLPATH);
					}
					// ����xml
					rootNode = xmlHandler.parseXml();
					// ��ȡ����
					List<XmlBean> childNodes = rootNode.getChildNodes("Organize");
					if (childNodes.size() == 0)
						throw new Exception("���ڵ���û���ҵ�Organize�ڵ㣬����XML�ļ���" + XMLPATH);
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
	 * �����ã�����xml��ʹ�ú���Ҫ����loadFunction
	 * @throws Exception
	 */
	public static void reloadXml() throws Exception {
		XmlHandler xmlHandler = new XmlHandler();
		// ����xml
		if (!xmlHandler.load(XMLPATH)) {
			throw new Exception("����xml�ļ�ʧ�ܣ�·����" + XMLPATH);
		}
		// ����xml
		rootNode = xmlHandler.parseXml();
		// ��ȡ����
		List<XmlBean> childNodes = rootNode.getChildNodes("Organize");
		if (childNodes.size() == 0)
			throw new Exception("���ڵ���û���ҵ�Organize�ڵ㣬����XML�ļ���" + XMLPATH);
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
	 * ���غ���������Ŀ�꺯���Ľڵ��Լ��ӽڵ�����
	 * @param commandId	TC�˵���CommandId
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
			throw new Exception("û���ҵ���ǰ�����Ӧ�ĺ�����CommandId=" + commandId);
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
	 * �������ݵ�SAP
	 * @param propMaps ������Ҫ����SAP�������
	 * @return	Function�¶����IsReturn=true��JCoTable�ļ���
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
	 * ��ʼ���������Ա����ֶ���Ϣ
	 * @throws Exception
	 */
	private void initTablesAndFields() throws Exception {
		importTable = null;
		outTables.clear();
		List<XmlBean> childNodes = functionNode.getChildNodes("Table");
		if (childNodes.size() == 0)
			throw new Exception("Function�ڵ���û���ҵ�Table�ڵ㣬����XML�ļ���" + XMLPATH);
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
			throw new Exception("Function�ڵ���û���ҵ���Ҫ��ImportTable\nFunctionName:"+this.functionName+"\nImportTable:"+this.importTableName);
		}
	}

	/**
	 * ����SAP
	 * @return
	 * @throws Exception
	 */
	private static JCoDestination connSap() throws Exception {
		// ����SAP
		JCoDestination dest = connector.getSAPDestination();
		if (dest == null)
			throw new Exception("����SAPʧ��");
		return dest;
	}

}
