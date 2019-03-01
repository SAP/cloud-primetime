package com.sap.primetime.adapters;

import javax.inject.Singleton;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.account.TenantContext;

@Singleton
public class NeoTenantProvider implements TenantProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(NeoTenantProvider.class);

	@Override
	public String getTenantId() {
		TenantContext context = getTenantContext();

		if (context != null) {
			return context.getTenant().getId();
		} else {
			return null;
		}
	}

	private static TenantContext getTenantContext() {
		try {
			Context ctx = new InitialContext();
			return (TenantContext) ctx.lookup("java:comp/env/TenantContext");
		} catch (NamingException e) {
			LOGGER.error("Error looking up TenantContext.", e);
			return null;
		}
	}

}
