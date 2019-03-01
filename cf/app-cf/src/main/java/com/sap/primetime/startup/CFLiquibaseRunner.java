package com.sap.primetime.startup;

import java.sql.SQLException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.PersistenceAdapter;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class CFLiquibaseRunner implements ServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(CFLiquibaseRunner.class);

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		try {
			Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
					new JdbcConnection(PersistenceAdapter.getDataSource().getConnection()));
			Liquibase liquibase = new liquibase.Liquibase("db/db.changelog.xml", new ClassLoaderResourceAccessor(),
					database);

			liquibase.update(new Contexts(), new LabelExpression());
		} catch (DatabaseException | SQLException e) {
			logger.error("Could not initialize datasource.", e);
		} catch (LiquibaseException e) {
			logger.error("Could not perform Liquibase update.", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

}
