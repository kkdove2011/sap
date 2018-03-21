package com.connor.sap.xml;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

public class XmlBean {
	private List<XmlBean> children=new ArrayList<>();
	private Element element;
	public XmlBean(Element element) {
		this.element=element;
	}
	public void addChild(XmlBean childBean) {
		this.children.add(childBean);
	}
	
	public List<XmlBean> getChildren(){
		return this.children;
	}
	
	public List<XmlBean> getChildNodes(String nodeName){
		List<XmlBean> resList=new ArrayList<>();
		for (XmlBean bean : this.children) {
			if(bean.getTagName().equals(nodeName)) {
				resList.add(bean);
			}
		}
		return resList;
	}
	
	public String getNodeValue(String propName) throws Exception {
		String res=this.element.attributeValue(propName);
		if(res==null) {
			throw new Exception("��ǩ"+getTagName()+"���������ԣ�"+propName);
		}
		return res;
	}
	
	public String getTagName() {
		return this.element.getName();
	}
}
