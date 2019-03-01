package com.sap.primetime.adapters;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

public interface DataSourceProvider {
	public DataSource getDatasource() throws NamingException;

	public Properties getProperties();
}
