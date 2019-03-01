package com.sap.primetime.adapters;

import java.util.Properties;

import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import com.sap.primetime.adapters.DataSourceProvider;
import com.sap.primetime.util.VcapServicesReader;

@Singleton
public class CFDataSourceProvider implements DataSourceProvider {
	@Override
	public DataSource getDatasource() throws NamingException {
		InitialContext ctx = new InitialContext();
		DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/DefaultDB");

		if (!(ds instanceof BasicDataSource)) {
			throw new IllegalArgumentException(
					"The data source is not an instance of type " + BasicDataSource.class.getName());
		}
		BasicDataSource bds = (BasicDataSource) ds;
		bds.setDriverClassName("org.postgresql.Driver");

		VcapServicesReader vcap = new VcapServicesReader();

		bds.setUrl("jdbc:postgresql://" + vcap.get("primetime-postgres", "hostname") + ":"
				+ vcap.get("primetime-postgres", "port") + "/" + vcap.get("primetime-postgres", "dbname"));
		bds.setUsername(vcap.get("primetime-postgres", "username"));
		bds.setPassword(vcap.get("primetime-postgres", "password"));

		return bds;
	}

	@Override
	public Properties getProperties() {
		return new Properties();
	}

}
