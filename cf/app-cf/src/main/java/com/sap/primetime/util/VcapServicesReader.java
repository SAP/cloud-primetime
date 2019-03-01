package com.sap.primetime.util;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VcapServicesReader {
	private HashMap<String, JsonObject> credentialsCache;

	public boolean containsService(String serviceName) {
		loadServiceDocument();
		return credentialsCache.containsKey(serviceName);
	}

	public String get(String serviceName, String paramName) {
		loadServiceDocument();

		JsonObject credentials = credentialsCache.get(serviceName);
		if (credentials == null) {
			throw new IllegalArgumentException("Failed to get credentials for service " + serviceName);
		}

		return credentials.get(paramName).getAsString();
	}

	private void loadServiceDocument() {
		synchronized (this) {
			if (credentialsCache == null) {
				credentialsCache = new HashMap<>();

				String servicesDocument = System.getenv("VCAP_SERVICES");
				if (StringUtils.isBlank(servicesDocument)) {
					throw new IllegalArgumentException("VCAP_SERVICES environment variable not found.");
				}

				JsonObject allServices = new JsonParser().parse(servicesDocument).getAsJsonObject();
				for (Entry<String, JsonElement> entry : allServices.entrySet()) {
					if (entry.getValue().isJsonArray()) {
						JsonArray services = entry.getValue().getAsJsonArray();

						for (int i = 0; i < services.size(); i++) {
							JsonElement singleService = services.get(i);
							JsonObject singleServiceObject = singleService.getAsJsonObject();
							String serviceName = singleServiceObject.get("name").getAsString();
							JsonObject credentials = singleServiceObject.get("credentials").getAsJsonObject();
							credentialsCache.put(serviceName, credentials);
						}
					}
				}
			}
		}
	}

}
