package com.sap.primetime.startup;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class SAMLAuthFilter extends AuthFilter {
	public SAMLAuthFilter() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (performAuth(request, response, "FORM")) {
			chain.doFilter(request, response);
		}
	}

}