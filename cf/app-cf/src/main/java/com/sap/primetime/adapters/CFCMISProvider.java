package com.sap.primetime.adapters;

import java.util.HashMap;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;

import com.sap.primetime.util.VcapServicesReader;

public class CFCMISProvider implements CMISProvider {
	// FIXME: get from environment, currently not available
	private static final String UNIQUE_NAME = "6a3a26a000563f61d76a3ea5";

	@Override
	public Session getCmisSession() {
		SessionFactory factory = SessionFactoryImpl.newInstance();
		Map<String, String> parameters = new HashMap<>();

		VcapServicesReader vcap = new VcapServicesReader();

		parameters.put(SessionParameter.USER, vcap.get("primetime-ecm", "user"));
		parameters.put(SessionParameter.PASSWORD, vcap.get("primetime-ecm", "password"));
		parameters.put(SessionParameter.BROWSER_URL, vcap.get("primetime-ecm", "uri"));
		parameters.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
		parameters.put(SessionParameter.REPOSITORY_ID, UNIQUE_NAME);

		return factory.createSession(parameters);
	}

}
