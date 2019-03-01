package com.sap.primetime.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.Config;

public class ConfigDAO extends BasicDAO<Config> {
	public static List<Config> getByGroup(String group) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		TypedQuery<Config> query = em.createNamedQuery(Config.QUERY_BYGROUP, Config.class);
		query.setParameter("group", group);
		List<Config> configs = query.getResultList();

		return configs;
	}

	public static boolean deleteConfig(String group, String key) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		int deleteCount = em.createNamedQuery(Config.QUERY_DELETE).setParameter("group", group).setParameter("key", key)
				.executeUpdate();

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return deleteCount > 0;
	}
}
