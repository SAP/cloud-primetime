package com.sap.primetime.adapters;

import org.apache.chemistry.opencmis.client.api.Session;

public interface CMISProvider {
	public Session getCmisSession();

}
