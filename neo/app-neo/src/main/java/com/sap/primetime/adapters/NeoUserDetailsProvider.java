package com.sap.primetime.adapters;

import java.security.Principal;

import javax.inject.Singleton;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.dao.entities.User;
import com.sap.security.auth.login.LoginContextFactory;
import com.sap.security.um.service.UserManagementAccessor;
import com.sap.security.um.user.PersistenceException;
import com.sap.security.um.user.UnsupportedUserAttributeException;
import com.sap.security.um.user.UserProvider;

@Singleton
public class NeoUserDetailsProvider implements UserDetailsProvider {
	private static final Logger logger = LoggerFactory.getLogger(NeoUserDetailsProvider.class);

	@Override
	public User getUserDetails(ServletRequest request) {
		User user = new User();
		com.sap.security.um.user.User idmUser = null;
		try {
			idmUser = getIdmUser(request);
		} catch (PersistenceException e) {
			logger.error("Could not get the user provider.", e);
		}

		if (idmUser != null) {
			String userName = idmUser.getName();
			user.setUserId(userName);

			try {
				user.setFirstName(idmUser.getAttribute("firstname"));
				user.setLastName(idmUser.getAttribute("lastname"));
				user.setEmail(idmUser.getAttribute("email"));
			} catch (UnsupportedUserAttributeException e) {
				logger.error("Could not get user's attribute.", e);
			}
		}

		return user;
	}

	private com.sap.security.um.user.User getIdmUser(ServletRequest request) throws PersistenceException {
		com.sap.security.um.user.User idmUser = null;
		Principal principal = ((HttpServletRequest) request).getUserPrincipal();
		if (principal != null) {
			// Read the currently logged in user from the user storage
			UserProvider provider = UserManagementAccessor.getUserProvider();
			idmUser = provider.getUser(principal.getName());
		}
		return idmUser;
	}

	@Override
	public boolean isAdmin(ServletRequest request) {
		return ((HttpServletRequest) request).isUserInRole("admin");
	}

	@Override
	public boolean isDBAdmin(ServletRequest request) {
		return ((HttpServletRequest) request).isUserInRole("dbadmin");
	}

	@Override
	public boolean logOut(HttpServletRequest request) {
		LoginContext loginContext = null;
		if (request.getRemoteUser() != null) {
			try {
				loginContext = LoginContextFactory.createLoginContext();
				loginContext.logout();
				return true;
			} catch (LoginException e) {
				return false;
			}
		}

		return true;
	}

}
