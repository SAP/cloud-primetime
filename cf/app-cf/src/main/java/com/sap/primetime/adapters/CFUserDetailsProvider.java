package com.sap.primetime.adapters;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.dao.entities.User;
import com.sap.xsa.security.container.XSUserInfo;
import com.sap.xsa.security.container.XSUserInfoException;

@Singleton
public class CFUserDetailsProvider implements UserDetailsProvider {
	private static final Logger logger = LoggerFactory.getLogger(CFUserDetailsProvider.class);

	@Override
	public User getUserDetails(ServletRequest request) {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		XSUserInfo userInfo = (XSUserInfo) httpRequest.getUserPrincipal();

		User user = new User();
		try {
			user.setUserId(userInfo.getLogonName());
			user.setEmail(userInfo.getEmail());
			user.setFirstName(userInfo.getGivenName().trim());
			user.setLastName(userInfo.getFamilyName().trim());
		} catch (XSUserInfoException e) {
			logger.error("Could not get user information.", e);
		}

		return user;
	}

	@Override
	public boolean isAdmin(ServletRequest request) {
		return isUserInRole(request, "admin");
	}

	@Override
	public boolean isDBAdmin(ServletRequest request) {
		return isUserInRole(request, "dbadmin");
	}

	private boolean isUserInRole(ServletRequest request, String role) {
		try {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			XSUserInfo userInfo = (XSUserInfo) httpRequest.getUserPrincipal();
			return userInfo.checkLocalScope(role);
		} catch (XSUserInfoException e) {
			logger.error("Could not get user information.", e);
			return false;
		}

	}

	@Override
	public boolean logOut(HttpServletRequest request) {
		// TODO: implement
		return false;
	}
}
