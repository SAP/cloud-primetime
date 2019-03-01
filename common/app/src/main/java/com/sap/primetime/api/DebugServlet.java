package com.sap.primetime.api;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/debug")
public class DebugServlet extends HttpServlet {
	private static final long serialVersionUID = 1375980381655546658L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");

		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = headerNames.nextElement();
			String value = request.getHeader(key);

			response.getOutputStream().println(key + ": " + value);
		}

		response.getOutputStream().println("principal: " + request.getUserPrincipal());
	}
}