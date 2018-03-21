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
	 * 发送数据到SAP
	 * @param destination
	 * @param functionName
	 * @param importTableName
	 * @param propMaps
	 * @param outTableNames
	 * @return 包含返回表的Map，如果包含key=@kSuccess，表示传送成功
	 * @throws Exception
	 */
	public static Map<String, JCoTable> sendData(JCoDestination destination, String functionName, String importTableName, List<Map<String, String>> propMaps, List<String> outTableNames)
			throws Exception {
		JCoFunction sapFunction = destination.getRepository().getFunction(functionName);
		if (null == sapFunction) {
			throw new Exception("没有从SAP系统中找到函数：" + functionName);
		}
		JCoContext.begin(destination);
		if (importTableName != null && !"".equals(importTableName)) {
			JCoTable importTable = sapFunction.getTableParameterList().getTable(importTableName);
			// 传入输入table的数据
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
		// 执行函数，传输数据
		sapFunction.execute(destination);
		// 获取需要返回的table
		Map<String, JCoTable> rtnTableMap = new HashMap<>();
		for (String outTableName : outTableNames) {
			rtnTableMap.put(outTableName, sapFunction.getTableParameterList().getTable(outTableName));
		}
		// 获取返回结果（RETURN表也可以配到XML）
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
	 * sapFunction) { throw new RuntimeException("没有从SAP系统中找到函数：" + functionName); }
	 * JCoContext.begin(destination); for (Entry<String, String> prop :
	 * propMap.entrySet()) {
	 * sapFunction.getImportParameterList().setValue(prop.getKey(),
	 * prop.getValue()); } // 执行函数，传输数据 sapFunction.execute(destination); //
	 * 获取返回的table JCoTable exportTable =
	 * sapFunction.getTableParameterList().getTable(returnTable); JCoTable
	 * stateTable = sapFunction.getTableParameterList().getTable("RETURN"); String
	 * result = stateTable.getString("TYPE"); JCoContext.end(destination); //
	 * S为传输成功，其他都是失败的 if ("S".equals(result)) { System.out.println("数据查询成功"); return
	 * exportTable; } return null; }
	 */

}
