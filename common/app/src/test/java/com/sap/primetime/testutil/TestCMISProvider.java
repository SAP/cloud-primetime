package com.sap.primetime.testutil;

import org.apache.chemistry.opencmis.client.api.Session;

import com.sap.primetime.adapters.CMISProvider;

public class TestCMISProvider implements CMISProvider {

	@Override
	public Session getCmisSession() {
		// TODO: instantiate InMemory or FileSystem binding
		return null;
	}

}
