package com.connor.sap.util;

import java.util.HashMap;
import java.util.Properties;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;

public class CustomDestinationDataProvider implements DestinationDataProvider {
	private static HashMap<String, Properties> providers = new HashMap<String, Properties>();
	@Override
	public Properties getDestinationProperties(String destName) {
		if (destName == null)
			throw new NullPointerException("��ָ��Ŀ������");
		if (providers.size() == 0)
			throw new IllegalStateException("�����һ��Ŀ�����Ӳ������Ը��ṩ��");
		return providers.get(destName);
	}

	@Override
	public boolean supportsEvents() {
		return false;
	}

	@Override
	public void setDestinationDataEventListener(DestinationDataEventListener listener) {
		throw new UnsupportedOperationException();
	}

	public void addDestinationProperties(String destName, Properties provider) {
		providers.put(destName, provider);
	}
}