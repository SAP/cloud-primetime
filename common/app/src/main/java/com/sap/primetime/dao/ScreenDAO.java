package com.sap.primetime.dao;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.Screen.AliveState;
import com.sap.primetime.dao.entities.User;

public class ScreenDAO extends BasicDAO<Screen> {
	private static final Logger logger = LoggerFactory.getLogger(ScreenDAO.class);

	public static List<Screen> getScreensForUser(User user) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Screen.QUERY_BYUSER, Screen.class).setParameter("user", user).getResultList();
	}

	public static Screen getScreenByKey(String key) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return getScreenByKey(key, em);
	}

	public static Screen getScreenByKey(String key, EntityManager em) {
		try {
			return em.createNamedQuery(Screen.QUERY_BYKEY, Screen.class).setParameter("key", key).getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (NonUniqueResultException e) {
			logger.error("Screen appears multiple times. This could mean severe database corruption. Fix immediately.",
					e);
			throw e;
		}
	}

	public static List<Screen> getOnboardings() {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Screen.QUERY_ONBOARDINGS, Screen.class).getResultList();
	}

	public static List<Screen> getScreenshotInstances(boolean activeOnly) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		List<Screen> screens = em.createNamedQuery(Screen.QUERY_SCREENSHOTINSTANCES, Screen.class).getResultList();
		if (activeOnly) {
			List<Screen> result = new ArrayList<>();
			for (Screen screen : screens) {
				if (screen.getAliveState() == AliveState.OK) {
					result.add(screen);
				}
			}
			return result;
		} else {
			return screens;
		}
	}

	public static List<Screen> getScreensByPlaylist(Playlist playlist) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Screen.QUERY_BYPLAYLIST, Screen.class).setParameter("playlist", playlist)
				.getResultList();
	}

	public static List<Screen> getActiveScreens() {
		EntityManager em = PersistenceAdapter.getEntityManager();

		Date threshold = Timestamp.valueOf(LocalDateTime.now().minus(Screen.DELAYED_THRESHOLD, ChronoUnit.SECONDS));
		return em.createNamedQuery(Screen.QUERY_ACTIVE, Screen.class).setParameter("thresholdDate", threshold)
				.getResultList();
	}
}
