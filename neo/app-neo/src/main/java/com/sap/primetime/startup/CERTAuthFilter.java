package com.sap.primetime.startup;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class CERTAuthFilter extends AuthFilter {
	public CERTAuthFilter() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (performAuth(request, response, "CERT")) {
			chain.doFilter(request, response);
		}
	}
}