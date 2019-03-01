package com.sap.primetime.testutil;

import javax.inject.Singleton;

import com.sap.primetime.adapters.TenantProvider;

@Singleton
public class TestTenantProvider implements TenantProvider {

	@Override
	public String getTenantId() {
		return "dev_defaultId";
	}

}
