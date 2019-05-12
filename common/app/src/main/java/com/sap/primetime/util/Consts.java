package com.sap.primetime.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.inject.Injector;

public class Consts {
	public static final String VERSION = "1.4.2";
	public static final int xDim = 350;
	public static final int yDim = 197;
	public static final int BUFFER_SIZE = 16 * 1024;
	public static final int BackendTimeout = 3000;

	public static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss Z";
	public static final DateTimeFormatter formatter = DateTimeFormat.forPattern(Consts.DATEFORMAT);

	public static final String APP = "app";
	public static final String PROP_ADMINISTRATORS = "administrators";
	public static final String PROP_USERIDPATTERN = "userIdPattern";
	public static final String PROP_CRON_DBCLEANUP = "cronDBCleanUp";
	public static final String PROP_APPURL = "appUrl";
	public static final String PROP_AVATARURL = "avatarUrl";
	public static final String PROP_PROFILEURL = "profileUrl";
	public static final String PROP_BUILDTIME = "buildTimestamp";
	public static final String PROP_FILESUPPORT = "fileSupport";
	public static final String PROP_LOGOUTURL = "logoutUrl";
	public static final String PROP_GETTINGSTARTEDURL = "gettingStartedUrl";
	public static final String PROP_DEVELOPMENTURL = "developmentUrl";
	public static final String PROP_NEWSURL = "newsUrl";
	public static final String PROP_ANALYTICSTOKEN = "analyticsToken";
	public static final String PROP_RECOMMENDEDAPPLETVAPPVERSION = "recommendedAppleTVAppVersion";
	public static final String PROP_INFOCONDITION = "infoCondition";
	public static final String PROP_INFOTEXT = "infoText";
	public static final String PROP_INFOLINK = "infoLink";

	public static final String APPLIANCE = "appliance";
	public static final String PROP_CONFIGREFRESHINTERVAL = "configRefreshInterval";
	public static final String PROP_RELOADINTERVAL = "reloadInterval";
	public static final String PROP_SCREENSHOTINTERVAL = "screenshotInterval";
	public static final String PROP_TECHNICALUSER = "technicalUser";

	// only needed for testing, actually not a good practice to store injector
	// globally
	public static Injector injector;
}
