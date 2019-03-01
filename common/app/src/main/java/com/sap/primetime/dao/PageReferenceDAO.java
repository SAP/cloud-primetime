package com.sap.primetime.dao;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.PageReference;

public class PageReferenceDAO extends BasicDAO<PageReference> {
	public int deleteByPage(Page page) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		Query query = em.createNamedQuery(PageReference.QUERY_DELETEBYPAGE).setParameter("page", page);
		int count = query.executeUpdate();

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return count;
	}
}
