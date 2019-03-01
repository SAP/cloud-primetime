package com.sap.primetime.dao.dto;

import java.util.Date;

public class ApplianceError {
	private String stacktrace;
	private Date timestamp;
	private String exception;
	private String reason;
	private String version;

	public ApplianceError() {
	}

	public ApplianceError(String stacktrace, Date timestamp, String exception, String reason, String version) {
		this.stacktrace = stacktrace;
		this.timestamp = timestamp;
		this.exception = exception;
		this.reason = reason;
		this.version = version;
	}

	public String getStacktrace() {
		return stacktrace;
	}

	public void setStacktrace(String stacktrace) {
		this.stacktrace = stacktrace;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getException() {
		return exception;
	}

	public void setException(String exception) {
		this.exception = exception;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
