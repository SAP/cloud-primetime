package com.sap.primetime.util.configuration;

import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.sap.primetime.dao.ConfigDAO;
import com.sap.primetime.dao.entities.Config;

public class ConfigDBAdapter implements ConfigAdapter {
	@Override
	public Properties getByGroup(String group) {
		Properties groupProperties = new Properties();

		List<Config> configs = ConfigDAO.getByGroup(group);
		for (Config config : configs) {
			if (StringUtils.isNotBlank(config.getParamKey())) {
				groupProperties.put(config.getParamKey(), config.getParamValue() == null ? "" : config.getParamValue());
			}
		}

		return groupProperties;
	}

	@Override
	public void save(String group, String key, String value) {
		Config config = new Config(group, key, value);
		ConfigDAO.save(config);
	}

	@Override
	public boolean remove(String group, String key) {
		return ConfigDAO.deleteConfig(group, key);
	}
}
