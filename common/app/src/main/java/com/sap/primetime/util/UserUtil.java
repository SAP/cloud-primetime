package com.sap.primetime.util;

import java.util.HashMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.User;

/**
 * Helper class to manage users.
 */
public class UserUtil {
	private static UserUtil instance = null;
	private static HashMap<Long, String> impersonation = new HashMap<>();

	public static UserUtil getInstance() {
		if (instance == null) {
			instance = new UserUtil();
		}
		return instance;
	}

	public synchronized User getLoggedInUser(ServletRequest request) {
		User user = getDetailsProvider().getUserDetails(request);

		// impersonate other user on demand
		if (isAdmin(request) && impersonation.containsKey(user.getId())) {
			user = UserDAO.getUserByUserId(impersonation.get(user.getId()));
		}

		return user;
	}

	public synchronized User getLoggedInDBUser(ServletRequest request) {
		User user = getLoggedInUser(request);

		User dbUser = UserDAO.getUserByUserId(user.getUserId());
		if (dbUser == null) {
			dbUser = UserDAO.saveNew(user);

			// FIXME: why is this needed, user should have executed this already
			dbUser.updateDynamicValues();
		} else {
			if ((user.getEmail() != null && !user.getEmail().equals(dbUser.getEmail()))
					|| (user.getUserId() != null && !user.getUserId().equals(dbUser.getUserId()))
					|| (user.getFirstName() != null && !user.getFirstName().equals(dbUser.getFirstName()))
					|| (user.getLastName() != null && !user.getLastName().equals(dbUser.getLastName()))) {
				// update user info
				dbUser.setUserId(user.getUserId());
				dbUser.setEmail(user.getEmail());
				dbUser.setFirstName(user.getFirstName());
				dbUser.setLastName(user.getLastName());
				dbUser = UserDAO.save(dbUser);
			}
		}

		return dbUser;
	}

	public synchronized User getDBUser(String userId) {
		userId = StringUtils.trim(userId);
		User dbUser = UserDAO.getUserByUserId(userId);
		if (dbUser == null) {
			dbUser = new User(userId);
			dbUser = UserDAO.saveNew(dbUser);
		}

		return dbUser;
	}

	public void impersonateUser(ServletRequest request, String userId) {
		impersonation.put(getLoggedInUser(request).getId(), userId);
	}

	public void stopImpersonation(ServletRequest request) {
		impersonation.remove(getDetailsProvider().getUserDetails(request).getId());
	}

	public String isImpersonating(ServletRequest request) {
		return isAdmin(request) ? impersonation.get(getDetailsProvider().getUserDetails(request).getId()) : null;
	}

	public boolean isAdmin(ServletRequest request) {
		return getDetailsProvider().isAdmin(request);
	}

	public boolean isDBAdmin(ServletRequest request) {
		return getDetailsProvider().isDBAdmin(request);
	}

	public boolean logOut(HttpServletRequest request) {
		return getDetailsProvider().logOut(request);
	}

	private UserDetailsProvider getDetailsProvider() {
		return Consts.injector.getInstance(UserDetailsProvider.class);
	}
}
