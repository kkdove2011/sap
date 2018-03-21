package com.connor.sap.util;

import java.util.ArrayList;
import java.util.List;

import com.connor.sap.xml.XmlBean;

public class SapDataTable {
	private String tableName;
	private boolean isReturn;
	private String tableSource;
	private List<SapDataField> sapDataFields;
	public SapDataTable(XmlBean tableNode) throws Exception {
		this.tableName=tableNode.getNodeValue("Name");
		System.out.println(tableName);
		if("true".equals(tableNode.getNodeValue("IsReturn").toLowerCase())) {
			this.isReturn=true;
		}else {
			this.isReturn=false;
		}
		this.tableSource=tableNode.getNodeValue("TableSource");
		getSapDataBeans(tableNode);
	}
	
	private void getSapDataBeans(XmlBean tableNode) throws Exception {
		List<XmlBean> childNodes = tableNode.getChildNodes("FieldConfig");
		sapDataFields=new ArrayList<>();
		for (XmlBean fieldNode : childNodes) {
			SapDataField bean=new SapDataField(fieldNode);
			if(!"".equals(bean.getTcFieldName())) {
				sapDataFields.add(bean);
			}
		}
	}
	
	public boolean isReturn() {
		return this.isReturn;
	}

	public String getTableName() {
		return tableName;
	}

	public String getTableSource() {
		return tableSource;
	}

	public List<SapDataField> getSapDataFields() {
		return sapDataFields;
	}
	
	
}
