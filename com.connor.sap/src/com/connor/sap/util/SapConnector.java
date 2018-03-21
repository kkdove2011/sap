package com.connor.sap.util;

import java.util.Properties;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

public class SapConnector implements ISapConnector {
	private static Properties SAPCONNECTION = new Properties();
	private static String SAP_CONN;

	@Override
	public void setDestinationName(String destinationName) {
		SAP_CONN = destinationName;
	}

	@Override
	public void setHost(String host) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_ASHOST, host);
	}

	@Override
	public void setSysNo(String sysNo) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_SYSNR, sysNo);
	}

	@Override
	public void setClient(String client) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_CLIENT, client);
	}

	@Override
	public void setUser(String user) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_USER, user);
	}

	@Override
	public void setPassword(String password) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_PASSWD, password);
	}

	@Override
	public void setLanguage(String language) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_LANG, language);
	}

	@Override
	public void setJcoPeakLimit(String peakLimit) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT, peakLimit);
	}

	@Override
	public void setJcoPoolCapacity(String capacity) {
		SAPCONNECTION.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY, capacity);
	}

	@Override
	public JCoDestination getSAPDestination() {
		try {
			JCoDestination dest = JCoDestinationManager.getDestination(SAP_CONN);
			System.out.println("���ӳɹ�");
			return dest;
		} catch (JCoException ex) {
			System.out.println(ex);
			System.out.println("����ʧ��");
			// ��������
			return RegetJcoDestination();
		}
	}

	public static JCoDestination RegetJcoDestination() {
		try {
			CustomDestinationDataProvider provider = new CustomDestinationDataProvider();
			provider.addDestinationProperties(SAP_CONN, SAPCONNECTION);
			Environment.registerDestinationDataProvider(provider);
			try {
				JCoDestination dest = JCoDestinationManager.getDestination(SAP_CONN);
				System.out.println("�������ӳɹ�");
				return dest;
			} catch (JCoException ex) {
				System.out.println(ex);
				System.out.println("��������ʧ�ܣ�"+ex.getMessage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("��������ʧ�ܣ�"+e.getMessage());
		}
		return null;
	}

}
