package com.sap.primetime.util.configuration;

import java.util.Properties;

public interface ConfigAdapter {
	public Properties getByGroup(String group);

	public void save(String group, String key, String value);

	public boolean remove(String group, String key);
}