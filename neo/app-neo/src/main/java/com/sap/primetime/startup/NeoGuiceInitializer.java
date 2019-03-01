package com.sap.primetime.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.sap.primetime.util.Consts;

public class NeoGuiceInitializer implements ServletContextListener {

	@Override
	public void contextInitialized(final ServletContextEvent sce) {
		Injector injector = Guice.createInjector(Stage.PRODUCTION, new NeoModule());
		sce.getServletContext().setAttribute(Injector.class.getName(), injector);
		Consts.injector = injector;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

}
