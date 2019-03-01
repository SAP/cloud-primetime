package com.sap.primetime.startup;

import java.io.IOException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.NotImplementedException;

import com.sap.security.auth.login.LoginContextFactory;

public abstract class AuthFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		throw new NotImplementedException("Specify auth type");
	}

	public boolean performAuth(ServletRequest request, ServletResponse response, String type) {
		HttpServletRequest req = (HttpServletRequest) request;
		String user = req.getRemoteUser();
		if (user == null) {
			LoginContext loginContext;
			try {
				loginContext = LoginContextFactory.createLoginContext(type);
				loginContext.login();
				return true;
			} catch (LoginException e) {
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				// TODO: need a better strategy, this currently spams the log
				// logger.error("Could not login user.", e);
				return false;
			}
		} else {
			return true;
		}
	}

	@Override
	public void init(FilterConfig fConfig) throws ServletException {
		return;
	}

	@Override
	public void destroy() {
		return;
	}
}