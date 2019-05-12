package com.sap.primetime.api;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;

import com.sap.primetime.adapters.PersistenceAdapter;
import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.dto.SystemInfo;
import com.sap.primetime.dao.entities.EventHistory;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigSection;
import com.sap.primetime.util.configuration.ConfigUtil;

@Path("/systemservice")
public class SystemService extends BasicService {
	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	public SystemInfo getSytemInfo() {
		SystemInfo result = new SystemInfo();

		String userId = userUtil.getLoggedInDBUser(request).getUserId();

		result.setBuildTime(ConfigUtil.getProperty(Consts.APP, Consts.PROP_BUILDTIME));
		result.setCurrentTime(new Date());
		result.setDefaultTimezone(Calendar.getInstance().getTimeZone().getID());
		result.setDateFormat(Consts.DATEFORMAT);
		result.setVersion(Consts.VERSION);
		result.setAnalyticsToken(ConfigUtil.getProperty(Consts.APP, Consts.PROP_ANALYTICSTOKEN));
		result.setxDim(Consts.xDim);
		result.setyDim(Consts.yDim);
		result.setCurrentUser(userId);
		result.setAdminMode(isAdmin() && userUtil.isImpersonating(request) == null);
		result.setDbAdminMode(isDBAdmin() && userUtil.isImpersonating(request) == null);
		result.setImpersonation(userUtil.isImpersonating(request));
		result.setLogoutUrl(ConfigUtil.getProperty(Consts.APP, Consts.PROP_LOGOUTURL));
		result.setGettingStartedUrl(ConfigUtil.getProperty(Consts.APP, Consts.PROP_GETTINGSTARTEDURL));
		result.setDevelopmentUrl(ConfigUtil.getProperty(Consts.APP, Consts.PROP_DEVELOPMENTURL));
		result.setNewsUrl(ConfigUtil.getProperty(Consts.APP, Consts.PROP_NEWSURL));
		result.setAdministrators(ConfigUtil.getProperty(Consts.APP, Consts.PROP_ADMINISTRATORS));
		result.setFileSupport(ConfigUtil.getBooleanProperty(Consts.APP, Consts.PROP_FILESUPPORT));
		result.setRecommendedAppleTVAppVersion(
				ConfigUtil.getProperty(Consts.APP, Consts.PROP_RECOMMENDEDAPPLETVAPPVERSION));
		result.setInfoCondition(ConfigUtil.getProperty(Consts.APP, Consts.PROP_INFOCONDITION));
		result.setInfoConditionMet(userId.matches(ConfigUtil.getProperty(Consts.APP, Consts.PROP_INFOCONDITION)));
		result.setInfoText(ConfigUtil.getProperty(Consts.APP, Consts.PROP_INFOTEXT));
		result.setInfoLink(ConfigUtil.getProperty(Consts.APP, Consts.PROP_INFOLINK));
		result.setConfigRefreshInterval(ConfigUtil.getIntProperty(Consts.APPLIANCE, Consts.PROP_CONFIGREFRESHINTERVAL));
		result.setReloadInterval(ConfigUtil.getIntProperty(Consts.APPLIANCE, Consts.PROP_RELOADINTERVAL));
		result.setScreenshotInterval(ConfigUtil.getIntProperty(Consts.APPLIANCE, Consts.PROP_SCREENSHOTINTERVAL));
		result.setTechnicalUser(ConfigUtil.getProperty(Consts.APPLIANCE, Consts.PROP_TECHNICALUSER));
		result.setAutoScreenshotInstances(ScreenDAO.getScreenshotInstances(true).size());
		result.setStats_playlists(new PlaylistDAO().getCount());
		result.setStats_pages(new PageDAO().getCount());
		result.setStats_files(new FileDAO().getCount());
		result.setStats_screens(new ScreenDAO().getCount());
		result.setStats_activescreens(ScreenDAO.getActiveScreens().size());

		return result;
	}

	@GET
	@Path("/sampledata")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createSampleData() {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		if (new PageDAO().getCount() > 0) {
			throwBadRequest("There is already data in the database.");
		}

		Page page1 = new Page("Page 1");
		page1.setPageType(PageType.TEXT);
		page1.setText("1");
		page1.setListPublicly(true);
		page1.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page1 = PageDAO.saveNew(page1);

		Page page2 = new Page("Page 2");
		page2.setPageType(PageType.TEXT);
		page2.setText("2");
		page2.setListPublicly(false);
		page2.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page2 = PageDAO.saveNew(page2);

		Page page3 = new Page("Page 3");
		page3.setPageType(PageType.TEXT);
		page3.setText("3");
		page3.setListPublicly(false);
		page3.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page3 = PageDAO.saveNew(page3);

		Page page4 = new Page("Page 4");
		page4.setPageType(PageType.TEXT);
		page4.setText("4");
		page4.setListPublicly(true);
		page4.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page4 = PageDAO.saveNew(page4);

		Page page5 = new Page("Page 5");
		page5.setPageType(PageType.TEXT);
		page5.setText("5");
		page5.setListPublicly(true);
		page5.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page5 = PageDAO.saveNew(page5);

		Page page6 = new Page("Page 6");
		page6.setPageType(PageType.TEXT);
		page6.setText("6");
		page6.setListPublicly(true);
		page6.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		page6 = PageDAO.saveNew(page6);

		// owner-less page
		Page page7 = new Page("Page 7");
		page7.setPageType(PageType.TEXT);
		page7.setText("7");
		page7.setListPublicly(true);
		page7 = PageDAO.saveNew(page7);

		Playlist playlist = new Playlist("Playlist 1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist.addPageReference(new PageReference(page3));
		playlist.addPageReference(new PageReference(page4));
		playlist.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		playlist = PlaylistDAO.saveNew(playlist);

		Playlist playlist2 = new Playlist("Playlist 2");
		playlist2.addPageReference(new PageReference(page5));
		playlist2.addPageReference(new PageReference(page6));
		playlist2.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		playlist2 = PlaylistDAO.saveNew(playlist2);

		Playlist playlist3 = new Playlist("Playlist 3");
		playlist3.addPageReference(new PageReference(page1));
		playlist3.addPageReference(new PageReference(playlist2));
		playlist3.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		playlist3 = PlaylistDAO.saveNew(playlist3);

		Playlist playlist4 = new Playlist("Playlist 4");
		playlist4.addPageReference(new PageReference(page1));
		playlist4.addPageReference(new PageReference(page2));
		playlist4.addPageReference(new PageReference(page3));
		playlist4.setListPublicly(true);
		playlist4 = PlaylistDAO.saveNew(playlist4);

		Screen screen1 = new Screen("screen1", playlist);
		screen1.setKey("k1");
		screen1.setMetric_applianceStarts(7);
		screen1.setMetric_applianceVersion("1.1.0");
		screen1.setMetric_applianceStartDate(new Date());
		screen1.setMetric_applianceLog(
				"0   CoreFoundation 0x0000000107dfb71b __exceptionPreprocess + 331\\n1   libobjc.A.dylib  0x000000010739a735 objc_exception_throw + 48\\n2   CoreFoundation 0x0000000107cd76dd -[__NSArray0 objectAtIndex:] + 93\\n3   PrimeTime Config 0x0000000106a6d8c8 -[AppDelegate sendScreenshot:] + 168\\n4   PrimeTime Config 0x0000000106a6c5d9 -[AppDelegate setScreenshotTimerToInterval:] + 425\\n5   PrimeTime Config 0x0000000106a6cb88 -[AppDelegate loadConfigFromLocation:] + 440\\n6   Foundation     0x0000000106e38a5b __NSThreadPerformPerform + 330\\n7   CoreFoundation 0x0000000107d60cb1 __CFRUNLOOP_IS_CALLING_OUT_TO_A_SOURCE0_PERFORM_FUNCTION__ + 17\\n8   CoreFoundation 0x0000000107d60523 __CFRunLoopDoSources0 + 243\\n9   CoreFoundation 0x0000000107d5abcf __CFRunLoopRun + 1263\\n10  CoreFoundation 0x0000000107d5a3a1 CFRunLoopRunSpecific + 625\\n11  GraphicsServices 0x000000010edf81dd GSEventRunModal + 62\\n12  UIKitCore      0x000000010a98b9d0 UIApplicationMain + 140\\n13  PrimeTime Config 0x0000000106a69a4f main + 127\\n14  libdyld.dylib  0x000000010974d7b1 start + 1\\n15  ???            0x0000000000000001 0x0 + 1");

		screen1.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		screen1 = ScreenDAO.saveNew(screen1);

		Screen screen2 = new Screen("screen2", playlist);
		screen2.addOwner(new Owner(userUtil.getLoggedInDBUser(request)));
		screen2.setLastAlive(new Date());
		screen2 = ScreenDAO.saveNew(screen2);

		// onboarding request
		Screen screen3 = new Screen();
		screen3.setKey("new-screen");
		screen3.setRequestedOwner("ExampleUserId");
		screen3.setMetric_user("ExampleUserId");
		screen3 = ScreenDAO.saveNew(screen3);

		// simulate statistics
		DateTime eventDate = new DateTime();
		for (int i = 0; i < 100; i++) {
			EventHistoryDAO.saveNew(new EventHistory(screen3.getId(), eventDate.toDate()));
			eventDate = eventDate.minusSeconds(ThreadLocalRandom.current().nextInt(15, 300));
		}

		// create more users
		UserDAO.saveNew(new User("k234", "k234@test.test", "John", "Doe"));
		User user1 = UserDAO.saveNew(new User("k123", "k123@test.test", "Maria", "Mustermann"));

		screen1.addOwner(new Owner(user1));
		screen1 = ScreenDAO.save(screen1);

		page3.addOwner(new Owner(user1));
		page3 = PageDAO.save(page3);

		return RESPONSE_OK;
	}

	@GET
	@Path("/configsections")
	@Produces(MediaType.APPLICATION_JSON)
	public List<ConfigSection> getConfigSections() {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		return ConfigUtil.getConfigSections(true);
	}

	@PUT
	@Path("/configsections/{group}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateConfigSection(ConfigSection section, @PathParam("group") String group) {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		if (ConfigUtil.saveConfiguration(group, section)) {
			reloadConfiguration();
			return RESPONSE_OK;
		}

		return RESPONSE_BAD;
	}

	@DELETE
	@Path("/configsections/{group}/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteDBConfigKey(@PathParam("group") String group, @PathParam("key") String key) {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		if (ConfigUtil.deleteConfiguration(group, key)) {
			reloadConfiguration();
			return RESPONSE_OK;
		}

		return RESPONSE_NOTFOUND;
	}

	@POST
	@Path("/reloadconfig")
	public Response reloadConfiguration() {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		ConfigUtil.reload();

		return RESPONSE_OK;
	}

	@POST
	@Path("/clearcache")
	@Produces(MediaType.APPLICATION_JSON)
	public Response clearCaches() {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		PersistenceAdapter.clearCache();

		return RESPONSE_OK;
	}
}
