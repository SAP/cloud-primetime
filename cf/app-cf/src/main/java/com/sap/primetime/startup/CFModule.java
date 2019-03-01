package com.sap.primetime.startup;

import com.google.inject.AbstractModule;
import com.sap.primetime.adapters.CFCMISProvider;
import com.sap.primetime.adapters.CFDataSourceProvider;
import com.sap.primetime.adapters.CFFeatureFlagsProvider;
import com.sap.primetime.adapters.CFTenantProvider;
import com.sap.primetime.adapters.CFUserDetailsProvider;
import com.sap.primetime.adapters.CMISProvider;
import com.sap.primetime.adapters.DataSourceProvider;
import com.sap.primetime.adapters.TenantProvider;
import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.util.configuration.FeatureFlagsProvider;

public class CFModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(UserDetailsProvider.class).to(CFUserDetailsProvider.class);
		bind(DataSourceProvider.class).to(CFDataSourceProvider.class);
		bind(CMISProvider.class).to(CFCMISProvider.class);
		bind(TenantProvider.class).to(CFTenantProvider.class);
		bind(FeatureFlagsProvider.class).to(CFFeatureFlagsProvider.class);
	}

}
