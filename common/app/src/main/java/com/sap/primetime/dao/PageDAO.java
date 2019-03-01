package com.sap.primetime.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.User;

public class PageDAO extends BasicDAO<Page> {
	public static List<Page> getPagesForUser(User user) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Page.QUERY_BYUSER, Page.class).setParameter("user", user).getResultList();
	}

	public static List<Page> getAdvertisedPages(int maxResults) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		TypedQuery<Page> query = em.createNamedQuery(Page.QUERY_ADVERTISED, Page.class);
		if (maxResults > 0) {
			query.setMaxResults(maxResults);
		}

		return query.getResultList();
	}

	public static List<Page> getNextPagesWithoutAutoScreenshot(int maxResults) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Page.QUERY_WITHOUTSCREENSHOT, Page.class).setMaxResults(maxResults).getResultList();
	}

	public static List<Page> getNextPageForAutoScreenshot(int maxResults) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Page.QUERY_AUTOSCREENSHOT, Page.class).setMaxResults(maxResults).getResultList();
	}

	public static List<Page> getPagesByFileOrWithTemplate(File file) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Page.QUERY_BYFILEORWITHTEMPLATE, Page.class).setParameter("file", file)
				.getResultList();
	}

	public static List<Page> getPageCatalog(User user) {
		List<Page> result = new ArrayList<>();
		EntityManager em = PersistenceAdapter.getEntityManager();

		List<Page> pages = em.createNamedQuery(Page.QUERY_CATALOG, Page.class).setParameter("user", user)
				.getResultList();

		// determine distinct set manually since HANA does not support distinct selects
		// with LOBs
		long lastId = 0;
		for (Page page : pages) {
			if (page.getId() != lastId) {
				result.add(page);
			}
			lastId = page.getId();
		}
		return result;
	}

}
