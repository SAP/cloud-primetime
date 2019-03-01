package com.sap.primetime.dao.dto;

import java.util.Date;

public class ApplianceInfo {
	private ApplianceError dump;
	private Date timestamp;
	private String version;

	public ApplianceInfo() {
	}

	public ApplianceInfo(ApplianceError dump, Date timestamp, String version) {
		this.dump = dump;
		this.timestamp = timestamp;
		this.version = version;
	}

	public ApplianceError getDump() {
		return dump;
	}

	public void setDump(ApplianceError dump) {
		this.dump = dump;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
