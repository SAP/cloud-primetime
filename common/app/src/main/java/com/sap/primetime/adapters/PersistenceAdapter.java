package com.sap.primetime.adapters;

import java.sql.SQLException;
import java.util.Properties;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.util.Consts;

public class PersistenceAdapter {
	private static final Logger logger = LoggerFactory.getLogger(PersistenceAdapter.class);
	private static EntityManagerFactory emf;

	public static EntityManager getEntityManager() {
		if (emf == null) {
			try {
				DataSourceProvider provider = Consts.injector.getInstance(DataSourceProvider.class);
				Properties properties = provider.getProperties();
				DataSource ds = getDataSource();
				if (ds != null) {
					String productName = ds.getConnection().getMetaData().getDatabaseProductName();
					logger.info("Database product name: " + productName);

					properties.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, ds);
				}
				emf = Persistence.createEntityManagerFactory("primetime", properties);
			} catch (SQLException e) {
				logger.error("Could not determine database type.", e);
			}
		}

		EntityManager em = emf.createEntityManager();

		// activate multi-tenancy if there is a tenant
		TenantProvider provider2 = Consts.injector.getInstance(TenantProvider.class);
		String tenantId = provider2.getTenantId();
		if (tenantId != null) {
			em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, tenantId);
		} else {
			em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, "-default-");
		}

		return em;
	}

	public static DataSource getDataSource() {
		DataSourceProvider provider = Consts.injector.getInstance(DataSourceProvider.class);

		try {
			return provider.getDatasource();
		} catch (NamingException e) {
			logger.error("JNDI datasource could not be found.", e);
			return null;
		}
	}

	public static synchronized void setService(EntityManagerFactory emFactory) {
		emf = emFactory;
	}

	public static synchronized void unsetService() {
		emf = null;
	}

	public static synchronized EntityManagerFactory getService() {
		return emf;
	}

	public static boolean beginTransactionOnDemand(EntityManager em) {
		boolean closeTransaction = false;

		if (!em.getTransaction().isActive()) {
			em.getTransaction().begin();
			closeTransaction = true;
		}

		return closeTransaction;
	}

	public static void commitTransactionOnDemand(EntityManager em, boolean closeTransaction) {
		if (closeTransaction && em.getTransaction().isActive()) {
			try {
				em.getTransaction().commit();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}
	}

	public static void clearCache() {
		emf.getCache().evictAll();
	}

}
