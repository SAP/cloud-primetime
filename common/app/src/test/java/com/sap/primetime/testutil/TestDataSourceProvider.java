package com.sap.primetime.testutil;

import static org.eclipse.persistence.config.PersistenceUnitProperties.DDL_GENERATION;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.LOGGING_LEVEL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TARGET_SERVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;

import java.util.Properties;

import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.eclipse.persistence.config.TargetServer;
import org.eclipse.persistence.logging.SessionLog;

import com.sap.primetime.adapters.DataSourceProvider;

@Singleton
public class TestDataSourceProvider implements DataSourceProvider {
	private static final String DB_PASSWORD = "";
	private static final String DB_USER = "";
	private static final String DB_DRIVER = "org.apache.derby.jdbc.ClientDriver";
	private static final String DB_CONN = "jdbc:derby:memory:PrimeTimeDB";
	private static final String DB_DDL = "drop-and-create-tables";

	@Override
	public DataSource getDatasource() throws NamingException {
		return null;
	}

	@Override
	public Properties getProperties() {
		Properties props = getJPAProperties();
		props.put(JDBC_URL, props.get(JDBC_URL) + ";create=true");

		return props;
	}

	private static Properties getJPAProperties() {
		Properties properties = new Properties();
		properties.put(TRANSACTION_TYPE, PersistenceUnitTransactionType.RESOURCE_LOCAL.name());
		properties.put(JDBC_DRIVER, DB_DRIVER);
		properties.put(JDBC_URL, DB_CONN);
		properties.put(JDBC_USER, DB_USER);
		properties.put(JDBC_PASSWORD, DB_PASSWORD);
		properties.put(LOGGING_LEVEL, SessionLog.INFO_LABEL);
		properties.put(TARGET_SERVER, TargetServer.None);
		properties.put(DDL_GENERATION, DB_DDL);

		return properties;
	}

}
