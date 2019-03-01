package com.sap.primetime.adapters;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.util.Consts;
import com.sap.primetime.util.VcapServicesReader;
import com.sap.primetime.util.configuration.FeatureFlagsProvider;

public class CFFeatureFlagsProvider implements FeatureFlagsProvider {
	private static final Logger logger = LoggerFactory.getLogger(CFFeatureFlagsProvider.class);

	@Override
	public Boolean getBooleanProperty(String group, String key) {
		HttpURLConnection urlConnection = null;

		try {
			VcapServicesReader vcap = new VcapServicesReader();

			String value = vcap.get("primetime-featureflags", "uri");
			URL url = new URL(value + "/api/v1/evaluate/" + key);

			String auth = Base64.getEncoder().encodeToString(new String(vcap.get("primetime-featureflags", "username")
					+ ":" + vcap.get("primetime-featureflags", "password")).getBytes());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + auth);
			urlConnection.setConnectTimeout(Consts.BackendTimeout);
			int code = urlConnection.getResponseCode();

			if (code == 200) {
				return true;
			} else if (code == 204) {
				return false;
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.warn("Error connecting to feature flags service. Marking feature " + group + "." + key
					+ " as unavailable.", e);
			return null;
		}
	}

}
