package com.sap.primetime.startup;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class BASICAuthFilter extends AuthFilter {
	public BASICAuthFilter() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (performAuth(request, response, "BASIC")) {
			chain.doFilter(request, response);
		}
	}
}