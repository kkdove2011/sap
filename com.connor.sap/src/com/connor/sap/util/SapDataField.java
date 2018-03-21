package com.connor.sap.util;

import com.connor.sap.xml.XmlBean;

public class SapDataField {
	private String sapFieldName;
	private String tcFieldName;
	private String relation;
	private String form;
	private boolean isEmpty;
	private String regex;
	private String defaultValue;
	private String formatter;
	private String sapDataType;
	private String sapDataLength;
	private String text;
	private String fieldType;
	public SapDataField(XmlBean fieldNode) throws Exception {
		this.sapFieldName=fieldNode.getNodeValue("SapField");
		this.tcFieldName=fieldNode.getNodeValue("PlmField");
		this.relation=fieldNode.getNodeValue("Relation");
		this.form=fieldNode.getNodeValue("Form");
		if("true".equalsIgnoreCase(fieldNode.getNodeValue("IsEmpty"))) {
			this.isEmpty=true;
		}else {
			this.isEmpty=false;
		}
		this.regex=fieldNode.getNodeValue("RegularExpression");
		this.defaultValue=fieldNode.getNodeValue("DefaultValue");
		this.formatter=fieldNode.getNodeValue("Format");
		this.sapDataType=fieldNode.getNodeValue("DataType");
		this.sapDataLength=fieldNode.getNodeValue("Length");
		this.text=fieldNode.getNodeValue("Text");
		this.fieldType=fieldNode.getNodeValue("FieldType");
	}
	public String getSapFieldName() {
		return sapFieldName;
	}
	public String getTcFieldName() {
		return tcFieldName;
	}
	public String getRelation() {
		return relation;
	}
	public String getForm() {
		return form;
	}
	public boolean isEmpty() {
		return isEmpty;
	}
	public String getRegex() {
		return regex;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public String getFormatter() {
		return formatter;
	}
	public String getSapDataType() {
		return sapDataType;
	}
	public String getSapDataLength() {
		return sapDataLength;
	}
	public String getText() {
		return text;
	}
	public String getFieldType() {
		return fieldType;
	}
	
	
}
