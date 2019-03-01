package com.sap.primetime.adapters;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.sap.primetime.dao.entities.User;

public interface UserDetailsProvider {
	public User getUserDetails(ServletRequest request);

	public boolean isAdmin(ServletRequest request);

	public boolean isDBAdmin(ServletRequest request);

	public boolean logOut(HttpServletRequest request);
}
