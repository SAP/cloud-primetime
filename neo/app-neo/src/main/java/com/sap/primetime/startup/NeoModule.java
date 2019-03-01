package com.sap.primetime.startup;

import com.google.inject.AbstractModule;
import com.sap.primetime.adapters.CMISProvider;
import com.sap.primetime.adapters.DataSourceProvider;
import com.sap.primetime.adapters.NeoCMISProvider;
import com.sap.primetime.adapters.NeoFeatureFlagsProvider;
import com.sap.primetime.adapters.NeoDataSourceProvider;
import com.sap.primetime.adapters.NeoTenantProvider;
import com.sap.primetime.adapters.NeoUserDetailsProvider;
import com.sap.primetime.adapters.TenantProvider;
import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.util.configuration.FeatureFlagsProvider;

public class NeoModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(UserDetailsProvider.class).to(NeoUserDetailsProvider.class);
		bind(DataSourceProvider.class).to(NeoDataSourceProvider.class);
		bind(CMISProvider.class).to(NeoCMISProvider.class);
		bind(TenantProvider.class).to(NeoTenantProvider.class);
		bind(FeatureFlagsProvider.class).to(NeoFeatureFlagsProvider.class);
	}

}
