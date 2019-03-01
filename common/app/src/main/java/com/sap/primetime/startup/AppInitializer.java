package com.sap.primetime.startup;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigDBAdapter;
import com.sap.primetime.util.configuration.ConfigSection;
import com.sap.primetime.util.configuration.ConfigSectionParam;
import com.sap.primetime.util.configuration.ConfigSectionParam.Type;
import com.sap.primetime.util.configuration.ConfigUtil;

public class AppInitializer implements ServletContextListener {
	private static Logger logger = LoggerFactory.getLogger(AppInitializer.class);

	public AppInitializer() {
	}

	@Override
	public void contextInitialized(ServletContextEvent context) {
		logger.info("Initializing Application");

		DateTimeZone.setDefault(DateTimeZone.UTC);
		initConfiguration();
		JobsInitializer.initScheduler();

		logger.info("Initialization Done");
	}

	public static void initConfiguration() {
		ConfigUtil.setDBAdapter(new ConfigDBAdapter());

		ConfigSection app = new ConfigSection(Consts.APP, "App Config");
		app.addParameter(new ConfigSectionParam(Consts.PROP_CRON_DBCLEANUP, "Cron DB Cleanup"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_ADMINISTRATORS, "Administrators"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_APPURL, "App URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_AVATARURL, "Avatar URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_PROFILEURL, "Profile URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_NEWSURL, "News URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_DEVELOPMENTURL, "Development URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_GETTINGSTARTEDURL, "Getting Started URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_LOGOUTURL, "Logout URL"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_FILESUPPORT, "File Support", Type.CHECKBOX));
		app.addParameter(new ConfigSectionParam(Consts.PROP_USERIDPATTERN, "User Id Pattern"));
		app.addParameter(new ConfigSectionParam(Consts.PROP_ANALYTICSTOKEN, "Analytics Token"));
		ConfigUtil.addConfigSection(app);

		ConfigSection appliance = new ConfigSection(Consts.APPLIANCE, "Appliance Config");
		appliance.addParameter(new ConfigSectionParam(Consts.PROP_CONFIGREFRESHINTERVAL, "Config Refresh Interval"));
		appliance.addParameter(new ConfigSectionParam(Consts.PROP_RELOADINTERVAL, "Browser Reload Interval"));
		appliance.addParameter(new ConfigSectionParam(Consts.PROP_SCREENSHOTINTERVAL, "Screenshot Interval"));
		appliance.addParameter(new ConfigSectionParam(Consts.PROP_TECHNICALUSER, "Technical User"));
		ConfigUtil.addConfigSection(appliance);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		JobsInitializer.shutDown();
	}

}
