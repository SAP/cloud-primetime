package com.sap.primetime.adapters;

import javax.inject.Singleton;

@Singleton
public class CFTenantProvider implements TenantProvider {

	@Override
	public String getTenantId() {
		// TODO: retrieve tenant from JWT token
		return null;
	}
}
