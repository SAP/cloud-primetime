package com.sap.primetime.util.configuration;

public interface FeatureFlagsProvider {
	public Boolean getBooleanProperty(String group, String key);
}
