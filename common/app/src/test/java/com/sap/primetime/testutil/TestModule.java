package com.sap.primetime.testutil;

import com.google.inject.AbstractModule;
import com.sap.primetime.adapters.CMISProvider;
import com.sap.primetime.adapters.DataSourceProvider;
import com.sap.primetime.adapters.TenantProvider;
import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.util.configuration.FeatureFlagsProvider;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(UserDetailsProvider.class).to(TestUserDetailsProvider.class);
		bind(DataSourceProvider.class).to(TestDataSourceProvider.class);
		bind(CMISProvider.class).to(TestCMISProvider.class);
		bind(TenantProvider.class).to(TestTenantProvider.class);
		bind(FeatureFlagsProvider.class).to(TestFeatureFlagsProvider.class);
	}

}
