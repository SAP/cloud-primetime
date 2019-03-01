package com.sap.primetime.dao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.User;

public class UserDAO extends BasicDAO<User> {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    public static User getUserByUserId(String userId) {
        EntityManager em = PersistenceAdapter.getEntityManager();

        return getUserByUserId(userId, em);
    }

    public static User getUserByUserId(String userId, EntityManager em) {
        try {
            return (User) em.createNamedQuery(User.QUERY_BYUSERID).setParameter("userId", userId).getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException e) {
            logger.error("User appears multiple times. This could mean severe database corruption. Fix immediately.", e);
            throw e;
        }
    }
}
