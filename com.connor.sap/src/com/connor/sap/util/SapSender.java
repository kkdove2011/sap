package com.connor.sap.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;

public class SapSender {
	/**
	 * �������ݵ�SAP
	 * @param destination
	 * @param functionName
	 * @param importTableName
	 * @param propMaps
	 * @param outTableNames
	 * @return �������ر��Map���������key=@kSuccess����ʾ���ͳɹ�
	 * @throws Exception
	 */
	public static Map<String, JCoTable> sendData(JCoDestination destination, String functionName, String importTableName, List<Map<String, String>> propMaps, List<String> outTableNames)
			throws Exception {
		JCoFunction sapFunction = destination.getRepository().getFunction(functionName);
		if (null == sapFunction) {
			throw new Exception("û�д�SAPϵͳ���ҵ�������" + functionName);
		}
		JCoContext.begin(destination);
		if (importTableName != null && !"".equals(importTableName)) {
			JCoTable importTable = sapFunction.getTableParameterList().getTable(importTableName);
			// ��������table������
			for (Map<String, String> propMap : propMaps) {
				if (!propMap.isEmpty()) {
					importTable.appendRow();
					importTable.lastRow();
					for (Entry<String, String> entity : propMap.entrySet()) {
						importTable.setValue(entity.getKey(), entity.getValue());
					}
				}
			}
			System.out.println("================ImportTable==================");
			System.out.println(importTable);
			System.out.println("=============================================");
		}
		// ִ�к�������������
		sapFunction.execute(destination);
		// ��ȡ��Ҫ���ص�table
		Map<String, JCoTable> rtnTableMap = new HashMap<>();
		for (String outTableName : outTableNames) {
			rtnTableMap.put(outTableName, sapFunction.getTableParameterList().getTable(outTableName));
		}
		// ��ȡ���ؽ����RETURN��Ҳ�����䵽XML��
		JCoTable stateTable = sapFunction.getTableParameterList().getTable("RETURN");
		rtnTableMap.put("RETURN", stateTable);
		System.out.println("===================RETURN====================");
		System.out.println(stateTable);
		System.out.println("=============================================");
		JCoContext.end(destination);
		return rtnTableMap;
	}

	/*
	 * public static JCoTable readData(JCoDestination destination, String
	 * functionName, String returnTable, Map<String, String> propMap) throws
	 * Exception { JCoFunction sapFunction =
	 * destination.getRepository().getFunction(functionName); if (null ==
	 * sapFunction) { throw new RuntimeException("û�д�SAPϵͳ���ҵ�������" + functionName); }
	 * JCoContext.begin(destination); for (Entry<String, String> prop :
	 * propMap.entrySet()) {
	 * sapFunction.getImportParameterList().setValue(prop.getKey(),
	 * prop.getValue()); } // ִ�к������������� sapFunction.execute(destination); //
	 * ��ȡ���ص�table JCoTable exportTable =
	 * sapFunction.getTableParameterList().getTable(returnTable); JCoTable
	 * stateTable = sapFunction.getTableParameterList().getTable("RETURN"); String
	 * result = stateTable.getString("TYPE"); JCoContext.end(destination); //
	 * SΪ����ɹ�����������ʧ�ܵ� if ("S".equals(result)) { System.out.println("���ݲ�ѯ�ɹ�"); return
	 * exportTable; } return null; }
	 */

}
