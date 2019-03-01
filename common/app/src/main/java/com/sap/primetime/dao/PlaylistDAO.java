package com.sap.primetime.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.User;

public class PlaylistDAO extends BasicDAO<Playlist> {
	private static final Logger logger = LoggerFactory.getLogger(PlaylistDAO.class);

	public static List<Playlist> getPlaylistsForUser(User user) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Playlist.QUERY_BYUSER, Playlist.class).setParameter("user", user).getResultList();
	}

	public static Playlist getPlaylistByName(String name) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		try {
			return em.createNamedQuery(Playlist.QUERY_BYNAME, Playlist.class).setParameter("name", name)
					.getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (NonUniqueResultException e) {
			logger.error(
					"Playlist appears multiple times. This could mean severe database corruption. Fix immediately.", e);
			throw e;
		}
	}

	public static Playlist getPlaylistByPageReference(PageReference ref) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		try {
			return em.createNamedQuery(Playlist.QUERY_BYPAGEREFERENCE, Playlist.class)
					.setParameter("pageReference", ref).getSingleResult();
		} catch (NoResultException e) {
			return null;
		} catch (NonUniqueResultException e) {
			logger.error(
					"PageReference appears multiple times. This could mean severe database corruption. Fix immediately.",
					e);
			throw e;
		}
	}

	public static List<Playlist> getPublicPlaylists() {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Playlist.QUERY_PUBLIC, Playlist.class).getResultList();
	}

	public static List<Playlist> getPlaylistCatalog(User user, long excludeId) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Playlist.QUERY_CATALOG, Playlist.class).setParameter("user", user)
				.setParameter("excludeId", excludeId).getResultList();
	}

	public static List<Playlist> getPlaylistsByPage(Page refPage) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Playlist.QUERY_BYPAGE, Playlist.class).setParameter("page", refPage).getResultList();
	}

	public static List<Playlist> getPlaylistsByPlaylist(Playlist refPlaylist) {
		EntityManager em = PersistenceAdapter.getEntityManager();

		return em.createNamedQuery(Playlist.QUERY_BYPLAYLIST, Playlist.class).setParameter("playlist", refPlaylist)
				.getResultList();
	}
}
