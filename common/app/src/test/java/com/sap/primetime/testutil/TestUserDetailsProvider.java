package com.sap.primetime.testutil;

import java.security.Principal;

import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.sap.primetime.adapters.UserDetailsProvider;
import com.sap.primetime.dao.entities.User;

@Singleton
public class TestUserDetailsProvider implements UserDetailsProvider {
	@Override
	public User getUserDetails(ServletRequest request) {
		Principal principal = ((HttpServletRequest) request).getUserPrincipal();
		return new User(principal.getName());
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
		return true;
	}

}
