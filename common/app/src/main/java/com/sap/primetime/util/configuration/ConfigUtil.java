package com.sap.primetime.util.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigSectionParam.Type;

public class ConfigUtil {
	private static final Logger logger = LoggerFactory.getLogger("audit.configuration");

	private static Map<String, Properties> allProperties = new HashMap<>();
	private static Map<String, Source> propertySource = new HashMap<>();
	private static Map<String, Properties> allPropertiesReset = new HashMap<>();
	private static Map<String, Source> propertySourceReset = new HashMap<>();
	private static List<Source> fallbacks = Arrays.asList(Source.DB, Source.ENV, Source.SYSTEM, Source.FILE);
	private static List<ConfigSection> configSections = new ArrayList<>();
	private static ConfigAdapter dbAdapter;

	// Feature flag handling
	private static FeatureFlagsProvider featureFlagProvider;
	private static Cache<String, Boolean> featureFlags = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
			.build();
	private static Cache<String, Boolean> unavailableFeatureFlags = CacheBuilder.newBuilder()
			.expireAfterWrite(5, TimeUnit.MINUTES).build();

	// Config options
	private static boolean enumeratePropertyFiles = true;
	private static boolean useFeatureFlagsService = false;

	public enum Source {
		FLAG, DB, FILE, SYSTEM, ENV, CODE
	}

	public static void setDBAdapter(ConfigAdapter dbAdapter) {
		ConfigUtil.dbAdapter = dbAdapter;
	}

	public static String getProperty(String group, String key) {
		Properties properties = getProperties(group);

		if (useFeatureFlagsService && getFeatureFlagValue(group, key) != null) {
			propertySource.put(group + "." + key, Source.FLAG);
			return getFeatureFlagValue(group, key).toString();
		} else if (properties != null && properties.getProperty(key) != null) {
			String result = properties.getProperty(key);
			return StringUtils.isAllBlank(result) ? null : result;
		} else {
			return null;
		}
	}

	public static String getResetProperty(String group, String key) {
		getProperties(group);
		return allPropertiesReset.get(group).getProperty(key);
	}

	/**
	 * Returns the source used for the last getProperty call
	 */
	public static Source getPropertySource(String group, String key) {
		return propertySource.get(group + "." + key);
	}

	public static Source getPropertyResetSource(String group, String key) {
		return propertySourceReset.get(group + "." + key);
	}

	private static Boolean getFeatureFlagValue(String group, String key) {
		if (Consts.injector == null) {
			return null;
		}
		if (featureFlagProvider == null) {
			featureFlagProvider = Consts.injector.getInstance(FeatureFlagsProvider.class);
		}
		String cacheKey = group + ":" + key;

		// return value if there is one
		if (featureFlags.asMap().containsKey(cacheKey)) {
			return featureFlags.getIfPresent(cacheKey);
		} else if (unavailableFeatureFlags.asMap().containsKey(cacheKey)) {
			return null;
		}

		// fetch current value
		Boolean value = featureFlagProvider.getBooleanProperty(group, key);
		if (value != null) {
			featureFlags.put(cacheKey, value);
		} else {
			unavailableFeatureFlags.put(cacheKey, true);
		}

		return value;
	}

	public static Boolean getBooleanProperty(String group, String key) {
		return Boolean.parseBoolean(getProperty(group, key));
	}

	public static int getIntProperty(String group, String key) {
		return Integer.parseInt(getProperty(group, key));
	}

	public static Properties getProperties(String group) {
		// load properties on demand
		if (!allProperties.containsKey(group)) {
			for (Source fallback : Lists.reverse(fallbacks)) {
				logger.info("Loading property group '" + group + "' from " + fallback.name());

				Properties properties = new Properties();

				switch (fallback) {
				case DB:
					properties = loadFromDatabase(group);
					break;
				case FILE:
					properties = loadFromPropertiesFile(group);
					break;
				case SYSTEM:
					properties = loadFromSystemProperties(group);
					break;
				case ENV:
					properties = loadFromEnvironment(group);
					break;
				default:
				}

				logger.info(" " + properties.size() + " properties loaded");
				saveProperties(group, properties, fallback);
			}
		}

		if (allProperties.containsKey(group)) {
			return allProperties.get(group);
		} else {
			return null;
		}
	}

	public static void setRuntimeProperty(String group, String key, String value) {
		Properties existing = allProperties.get(group);
		if (existing == null) {
			existing = getProperties(group);
			if (existing == null) {
				existing = new Properties();
			}
		}
		existing.put(key, value);
		allProperties.put(group, existing);
		propertySource.put(group + "." + key, Source.CODE);
	}

	private static Properties loadFromPropertiesFile(String group) {
		Properties properties = new Properties();
		int fileCounter = 1;

		String fileName = group + ".properties";
		String fileContents = null;
		do {
			try {
				fileContents = IOUtils.resourceToString("/" + fileName, StandardCharsets.UTF_8);
			} catch (IOException e) {
				break;
			}

			if (fileContents != null) {
				try (InputStream stream = IOUtils.toInputStream(fileContents, StandardCharsets.UTF_8)) {
					properties.load(stream);
				} catch (IOException e) {
					throw new RuntimeException("Failed to read configuration from resource at '" + fileName + "'.", e);
				}
			}

			if (!isEnumeratePropertyFiles()) {
				break;
			}

			fileCounter += 1;
			fileName = group + "." + fileCounter + ".properties";
		} while (fileContents != null);

		// overwrite with runtime specified config file ondemand
		String configName = System.getenv("primeTimeConfig");
		if (configName != null) {
			fileName = group + "." + configName + ".properties";
			try {
				fileContents = IOUtils.resourceToString("/" + fileName, StandardCharsets.UTF_8);
				try (InputStream stream = IOUtils.toInputStream(fileContents, StandardCharsets.UTF_8)) {
					properties.load(stream);
				} catch (IOException e) {
					throw new RuntimeException("Failed to read configuration from resource at '" + fileName + "'.", e);
				}
			} catch (IOException e) {
				logger.error("Config file " + fileName + " not found.");
			}
		}

		return properties;
	}

	private static Properties loadFromSystemProperties(String group) {
		Properties groupProperties = new Properties();
		Properties properties = System.getProperties();

		String prefix = group + ".";

		for (String key : properties.stringPropertyNames()) {
			if (key.startsWith(prefix)) {
				String value = properties.getProperty(key);

				groupProperties.put(key.substring(prefix.length()), value);
			}
		}

		return groupProperties;
	}

	private static Properties loadFromEnvironment(String group) {
		Properties groupProperties = new Properties();
		Map<String, String> properties = System.getenv();

		String prefix = group + ".";

		for (String key : properties.keySet()) {
			if (key.startsWith(prefix)) {
				String value = properties.get(key);

				groupProperties.put(key.substring(prefix.length()), value);
			}
		}

		return groupProperties;
	}

	private static Properties loadFromDatabase(String group) {
		if (dbAdapter != null) {
			return dbAdapter.getByGroup(group);
		} else {
			logger.error("Cannot load configuration from database. DB adapter is not set.");
			return new Properties();
		}
	}

	private static void saveProperties(String group, Properties properties, Source source) {
		if (properties.size() == 0) {
			return;
		}

		if (allProperties.containsKey(group)) {
			Properties existingProperties = allProperties.get(group);

			for (String key : properties.stringPropertyNames()) {
				existingProperties.put(key, properties.getProperty(key));
				propertySource.put(group + "." + key, source);
			}
		} else {
			allProperties.put(group, properties);
			for (String key : properties.stringPropertyNames()) {
				propertySource.put(group + "." + key, source);
			}
		}

		if (source != Source.DB) {
			Properties propCopy = new Properties();
			propCopy.putAll(allProperties.get(group));
			allPropertiesReset.put(group, propCopy);

			for (Object key : allProperties.get(group).keySet()) {
				propertySourceReset.put(group + "." + key, propertySource.get(group + "." + key));
			}
		}
	}

	public static List<Source> getFallbackChain() {
		return fallbacks;
	}

	public static void setFallbacks(List<Source> newFallbacks) {
		fallbacks = newFallbacks;
	}

	public static void reload() {
		logger.info("Reloading configuration");
		allProperties.clear();
		allPropertiesReset.clear();
		propertySource.clear();
		propertySourceReset.clear();
		featureFlags.invalidateAll();
		unavailableFeatureFlags.invalidateAll();
	}

	public static Map<String, Properties> getAllProperties() {
		return allProperties;
	}

	public static List<ConfigSection> getConfigSections() {
		return getConfigSections(false);
	}

	public static List<ConfigSection> getConfigSections(boolean fillCurrentValues) {
		for (ConfigSection section : configSections) {
			for (ConfigSectionParam param : section.getParameters()) {
				if (fillCurrentValues && !param.getType().equals(Type.PASSWORD)) {
					param.setCurrentValue(getProperty(section.getKey(), param.getKey()));
					param.setResetValue(getResetProperty(section.getKey(), param.getKey()));
				} else {
					param.setCurrentValue(null);
					param.setResetValue(null);
				}
				param.setSource(getPropertySource(section.getKey(), param.getKey()));
				param.setResetSource(getPropertyResetSource(section.getKey(), param.getKey()));
			}
		}
		return configSections;
	}

	public static void setConfigSections(List<ConfigSection> configSections) {
		ConfigUtil.configSections = configSections;
	}

	public static void addConfigSection(ConfigSection config) {
		configSections.add(config);
	}

	public static void clearConfigSections() {
		configSections.clear();
	}

	/**
	 * Saves the configuration into the database.
	 */
	public static boolean saveConfiguration(String group, ConfigSection section) {
		if (dbAdapter == null) {
			logger.error("Cannot save configuration. DB adapter is not set.");
			return false;
		}

		for (ConfigSectionParam param : section.getParameters()) {
			dbAdapter.save(group, param.getKey(), param.getCurrentValue());
		}

		return true;
	}

	/**
	 * Removes a configuration value from the database.
	 */
	public static boolean deleteConfiguration(String group, String key) {
		if (dbAdapter == null) {
			logger.error("Cannot delete configuration. DB adapter is not set.");
			return false;
		}

		return dbAdapter.remove(group, key);
	}

	public static boolean isEnumeratePropertyFiles() {
		return enumeratePropertyFiles;
	}

	public static void setEnumeratePropertyFiles(boolean enumeratePropertyFiles) {
		ConfigUtil.enumeratePropertyFiles = enumeratePropertyFiles;
	}
}
