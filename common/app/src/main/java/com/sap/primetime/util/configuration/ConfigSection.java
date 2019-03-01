package com.sap.primetime.util.configuration;

import java.util.ArrayList;
import java.util.List;

public class ConfigSection {
	private String key;
	private String name;
	private List<ConfigSectionParam> parameters = new ArrayList<>();

	public ConfigSection() {
	}

	public ConfigSection(String key, String name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ConfigSectionParam> getParameters() {
		return parameters;
	}

	public void setParameters(List<ConfigSectionParam> parameters) {
		this.parameters = parameters;
	}

	public void addParameter(ConfigSectionParam param) {
		parameters.add(param);
	}

	@Override
	public String toString() {
		return "ConfigSection [key=" + key + ", name=" + name + ", parameters=" + parameters + "]";
	}

}
