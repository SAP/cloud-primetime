package com.sap.primetime.util.configuration;

import com.sap.primetime.util.configuration.ConfigUtil.Source;

public class ConfigSectionParam {
	public enum Type {
		TEXT, PASSWORD, CHECKBOX, TEXTAREA
	}

	private String name;
	private String key;
	private String currentValue;
	private String resetValue;
	private Type type = Type.TEXT;
	private Source source;
	private Source resetSource;

	public ConfigSectionParam() {
	}

	public ConfigSectionParam(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public ConfigSectionParam(String key, String name, Type type) {
		this(key, name);

		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}

	public String getResetValue() {
		return resetValue;
	}

	public void setResetValue(String resetValue) {
		this.resetValue = resetValue;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public Source getResetSource() {
		return resetSource;
	}

	public void setResetSource(Source resetSource) {
		this.resetSource = resetSource;
	}

	@Override
	public String toString() {
		return "ConfigSectionParam [name=" + name + ", key=" + key + ", currentValue=" + currentValue + ", type=" + type
				+ "]";
	}

}
