package com.connor.sap.xml;

import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class XmlHandler{

	private Document doc;
	
	public boolean load(String fileName) {
		try {
			SAXReader saxReader=new SAXReader();
			this.doc=saxReader.read(fileName);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public XmlBean parseXml() {
		if(doc==null) {
			return null;
		}
		Element rootNode=doc.getRootElement();
		XmlBean rootBean=new XmlBean(rootNode);
		getChildNodes(rootBean,rootNode);
		return rootBean;
	}

	private void getChildNodes(XmlBean parentBean,Element parentNode) {
		Iterator<?> i=parentNode.elementIterator();
		XmlBean childBean;
		Element childNode;
		while(i.hasNext()) {
			childNode=(Element) i.next();
			childBean=new XmlBean(childNode);
			parentBean.addChild(childBean);
			getChildNodes(childBean, childNode);
		}
	}

}
