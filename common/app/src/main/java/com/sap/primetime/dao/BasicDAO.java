package com.sap.primetime.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.User;

@SuppressWarnings("unchecked")
public class BasicDAO<T> {
	public static <T> T save(T t) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		T merge = em.merge(t);

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return merge;
	}

	public static <T> T save(T t, EntityManager em) {
		T merge = em.merge(t);

		return merge;
	}

	/**
	 * An error safe version of {@link #save} which does not return the saved entity
	 * but rather if it could be saved. This is useful when handling items with
	 * unique keys.
	 */
	public static <T> boolean trySave(T t) {
		try {
			save(t);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static <T> T saveNew(T t) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		em.persist(t);

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return t;
	}

	public static <T> void saveNew(T t, EntityManager em) {
		em.persist(t);
	}

	public T getById(long id) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		T t = getById(id, em);

		return t;
	}

	public T getById(long id, EntityManager em) {
		T t = null;

		try {
			Query query = em.createQuery("select u from " + getTableName() + " u where u.id = :id");
			query.setParameter("id", id);
			t = (T) query.getSingleResult();
		} catch (NoResultException e) {
			// no need to log an error
		}

		return t;
	}

	public T getSingleByField(String field, Object value) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		T t = null;

		try {
			Query query = em.createQuery("select u from " + getTableName() + " u where u." + field + " = :value");
			query.setParameter("value", value);
			t = (T) query.getSingleResult();
		} catch (NoResultException e) {
			// no need to log an error
		}

		return t;
	}

	public List<T> getByField(String field, Object value, String orderBy) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		Query query = em.createQuery(
				"select u from " + getTableName() + " u where u." + field + " = :value order by u." + orderBy);
		query.setParameter("value", value);
		List<T> result = query.getResultList();

		return result;
	}

	public long getCount() {
		EntityManager em = PersistenceAdapter.getEntityManager();

		TypedQuery<Long> query = em.createQuery("select count(u) from " + getTableName() + " u", Long.class);
		Long result = query.getSingleResult();

		return result;
	}

	public List<T> getAll() {
		EntityManager em = PersistenceAdapter.getEntityManager();

		Query query = em.createQuery("select u from " + getTableName() + " u");
		List<T> result = query.getResultList();

		return result;
	}

	public List<T> getAll(String orderBy) {
		return getAll(orderBy, true);
	}

	public List<T> getAll(String orderBy, boolean stringBased) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		Query query;
		if (stringBased) {
			query = em.createQuery("select u from " + getTableName() + " u order by lower(u." + orderBy + ")");
		} else {
			query = em.createQuery("select u from " + getTableName() + " u order by u." + orderBy);
		}
		List<T> result = query.getResultList();

		return result;
	}

	public List<T> getAllDesc(String orderBy) {
		return getAllDesc(orderBy, true);
	}

	public List<T> getAllDesc(String orderBy, boolean stringBased) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		Query query;
		if (stringBased) {
			query = em.createQuery("select u from " + getTableName() + " u order by lower(u." + orderBy + ") desc");
		} else {
			query = em.createQuery("select u from " + getTableName() + " u order by u." + orderBy + " desc");
		}
		List<T> result = query.getResultList();

		return result;
	}

	/**
	 * Deletes all items for the entity.
	 */
	public int deleteAll() {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		Query query = em.createQuery("delete from " + getTableName());
		int count = query.executeUpdate();

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return count;
	}

	/**
	 * Deletes all items for the entity after a certain date.
	 */
	public int deleteAll(DateTime afterDate) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		Query query = em.createQuery("delete from " + getTableName() + " where dateCreated > :date");
		query.setParameter("date", afterDate.toDate());

		try {
			int count = query.executeUpdate();
			PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

			return count;
		} catch (Exception e) {
			// logger.warn("Could not delete all records for table '" +
			// getTableName() + "' after '" + afterDate + "'");
			return -1;
		}
	}

	/**
	 * Deletes all items for the entity before a certain date.
	 */
	public int deleteAllBefore(DateTime beforeDate) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);

		Query query = em.createQuery("delete from " + getTableName() + " where dateCreated < :date");
		query.setParameter("date", beforeDate.toDate());

		try {
			int count = query.executeUpdate();
			PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

			return count;
		} catch (Exception e) {
			// logger.warn("Could not delete all records for table '" +
			// getTableName() + "' after '" + afterDate + "'");
			return -1;
		}
	}

	/**
	 * Deletes an item through its primary key id.
	 */
	public boolean deleteById(long id) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);
		boolean deleted = false;

		Query query = em.createQuery("delete from " + getTableName() + " t where t.id = :id");
		query.setParameter("id", id);
		deleted = query.executeUpdate() > 0;

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return deleted;
	}

	/**
	 * Deletes an item through a custom field.
	 */
	public boolean deleteByField(String field, Object key) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);
		boolean deleted = false;

		Query query = em.createQuery("delete from " + getTableName() + " t where t." + field + " = :value");
		query.setParameter("value", key);
		deleted = query.executeUpdate() > 0;

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return deleted;
	}

	/**
	 * Deletes an item through it's primary key id double checking with the user to
	 * avoid misuse.
	 */
	public boolean deleteById(long id, User user) {
		EntityManager em = PersistenceAdapter.getEntityManager();
		boolean closeTransaction = PersistenceAdapter.beginTransactionOnDemand(em);
		boolean deleted = false;

		Query query = em.createQuery("delete from " + getTableName() + " t where t.id = :id and t.user = :user");
		query.setParameter("id", id);
		query.setParameter("user", user);
		deleted = query.executeUpdate() > 0;

		PersistenceAdapter.commitTransactionOnDemand(em, closeTransaction);

		return deleted;
	}

	private Type getActualType() {
		Type genericSuperclass = this.getClass().getGenericSuperclass();
		ParameterizedType pt = (ParameterizedType) genericSuperclass;
		Type type = pt.getActualTypeArguments()[0];

		return type;
	}

	private String getTableName() {
		String[] arr = StringUtils.split(getActualType().toString(), ".");

		return arr[arr.length - 1];
	}
}
