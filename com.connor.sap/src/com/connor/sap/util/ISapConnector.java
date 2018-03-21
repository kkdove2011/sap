package com.connor.sap.util;

import com.sap.conn.jco.JCoDestination;

public interface ISapConnector {
	public JCoDestination getSAPDestination();
	public void setDestinationName(String destinationName);
	public void setHost(String host);
	public void setSysNo(String sysNo);
	public void setClient(String client);
	public void setUser(String user);
	public void setPassword(String password);
	public void setLanguage(String language);
	public void setJcoPeakLimit(String peakLimit);
	public void setJcoPoolCapacity(String capacity);
}
