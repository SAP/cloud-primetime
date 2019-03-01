package com.sap.primetime.dao;

import java.util.List;

import javax.persistence.EntityManager;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.User;

public class FileDAO extends BasicDAO<File> {
	public static List<File> getFilesForUser(User user) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(File.QUERY_BYUSER, File.class).setParameter("user", user).getResultList();
	}
}
