package com.sap.primetime.adapters;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.FeatureFlagsProvider;

public class NeoFeatureFlagsProvider implements FeatureFlagsProvider {
	private static final Logger logger = LoggerFactory.getLogger(NeoFeatureFlagsProvider.class);

	@Override
	public Boolean getBooleanProperty(String group, String key) {
		HttpURLConnection urlConnection = null;

		try {
			Context ctx = new InitialContext();
			ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx
					.lookup("java:comp/env/connectivityConfiguration");

			DestinationConfiguration destConfiguration = configuration.getConfiguration("featureflags");
			if (destConfiguration == null) {
				return null;
			}

			String value = destConfiguration.getProperty("URL");
			URL url = new URL(value + "/api/v1/evaluate/" + key);

			String auth = Base64.getEncoder().encodeToString(
					new String(destConfiguration.getProperty("User") + ":" + destConfiguration.getProperty("Password"))
							.getBytes());
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
			logger.error("Error connecting to feature flags service.", e);
			return null;
		}
	}

}
