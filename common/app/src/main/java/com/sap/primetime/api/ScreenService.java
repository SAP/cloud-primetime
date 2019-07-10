package com.sap.primetime.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PageReferenceDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.ApplianceInfo;
import com.sap.primetime.dao.entities.EventHistory;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigUtil;

import io.swagger.annotations.Api;

@Api(value = "Screens")
@Path("screenservice")
public class ScreenService extends BasicService implements OwnerAPISupport<Screen> {
	private static final Logger logger = LoggerFactory.getLogger(ScreenService.class);

	private Cache<Long, Integer> remoteControlKeys = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	@GET
	@Path("/screens")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Screen> getScreens(@QueryParam("userId") String userId) {
		List<Screen> screens = ScreenDAO.getScreensForUser(getForeignUser(userId));
		List<Screen> result = new ArrayList<>();

		for (Screen screen : screens) {
			if (screen.getPlaylist() != null) {
				screen.getPlaylist().expand();
			}

			result.add(screen);
		}

		return result;
	}

	@GET
	@Path("/onboardings")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Screen> getOnboardings() {
		if (!isAdmin()) {
			throwUnauthorized();
		}

		return ScreenDAO.getOnboardings();
	}

	@GET
	@Path("/screens/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen getScreen(@PathParam("id") long screenId,
			@DefaultValue("true") @QueryParam("includeMetrics") boolean includeMetrics,
			@DefaultValue("false") @QueryParam("live") boolean live) {
		Screen result = null;

		if (screenId == 0) {
			result = new Screen();
			result.setName("Demo Screen");
			result.setShowOwners(true);

			User user = new User("d046998", "robert.wetzold@sap.com", "Robert", "Wetzold");
			user.updateDynamicValues();

			File movie = new File("-PrimeTime Teaser-");
			movie.setId(-1);

			Playlist playlist = new Playlist();

			Page page0 = new Page("Introduction");
			page0.addOwner(new Owner(user));
			page0.setPageType(PageType.MOVIE);
			page0.setFile(movie);
			page0.updateDynamicValues();

			PageReference pageRef0 = new PageReference(page0);
			pageRef0.setPageDisplayDurationOverride(12);
			playlist.addPageReference(pageRef0);

			Page page1 = new Page("Introduction");
			page1.addOwner(new Owner(user));
			page1.setPageType(PageType.TEXT);
			page1.setText("This is a demo for PrimeTime, your simple solution to create wall screen presentations.");
			page1.updateDynamicValues();
			playlist.addPageReference(new PageReference(page1));

			Page page2 = new Page("Getting Started");
			page2.addOwner(new Owner(user));
			page2.setPageType(PageType.TEXT);
			page2.setText(
					"To get started, create your pages and put them together into your own playlist.</p><p>You can show one ore multiple URLs, PDFs, images, movies, formatted texts or HTML.");
			page2.updateDynamicValues();
			playlist.addPageReference(new PageReference(page2));

			Page page3 = new Page("Contribute");
			page3.addOwner(new Owner(user));
			page3.setPageType(PageType.TEXT);
			page3.setText(
					"This is an open source tool by SAP. If you want to contribute, please feel invited to do so.");
			page3.updateDynamicValues();
			playlist.addPageReference(new PageReference(page3));

			String devUrl = ConfigUtil.getProperty(Consts.APP, Consts.PROP_DEVELOPMENTURL);
			if (StringUtils.isNoneEmpty(devUrl)) {
				Page page4 = new Page("Source Code");
				page4.addOwner(new Owner(user));
				page4.setUrl(devUrl);
				page4.updateDynamicValues();
				playlist.addPageReference(new PageReference(page4));
			}

			result.setPlaylist(playlist);
		} else {
			result = getExistingScreen(screenId, false);
			checkRemoteValidity(result);

			if (result.getOwners().isEmpty() && !isAdmin()) {
				throwUnauthorized();
			}
			if (result.isScreenshotMode() && live) {
				result.setPlaylist(getScreenshotPlaylist());
				result.setShowHeader(false);
				result.setShowFooter(false);
			}
			if (result.getPlaylist() != null) {
				result.getPlaylist().expand();
			}

			if (!includeMetrics) {
				removeMetrics(result);
			}
		}
		return result;
	}

	@GET
	@Path("/screens/bykey/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen getScreenByKey(@PathParam("key") String key,
			@DefaultValue("true") @QueryParam("includeMetrics") boolean includeMetrics,
			@DefaultValue("false") @QueryParam("live") boolean live,
			@DefaultValue("false") @QueryParam("autoOnboard") boolean doOnboarding,
			@QueryParam("owner") String requestedOwner) {
		Screen existingScreen = ScreenDAO.getScreenByKey(key);
		if (existingScreen == null) {
			if (doOnboarding) {
				existingScreen = new Screen();
				User user = userUtil.getLoggedInDBUser(request);
				existingScreen.setMetric_user(user.getUserId());
				existingScreen.setKey(key);
				if (StringUtils.isEmpty(requestedOwner)) {
					existingScreen.setRequestedOwner(user.getUserId());
				} else {
					existingScreen.setRequestedOwner(requestedOwner);
				}

				existingScreen = ScreenDAO.saveNew(existingScreen);
			} else {
				throwNotFound();
			}
		} else {
			checkRemoteValidity(existingScreen);
		}

		if (existingScreen.getOwners().isEmpty()) {
			addOnboardingScreen(existingScreen);
		}
		if (existingScreen.isScreenshotMode() && live) {
			existingScreen.setPlaylist(getScreenshotPlaylist());
			existingScreen.setShowHeader(false);
			existingScreen.setShowFooter(false);
		}
		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		if (!includeMetrics) {
			removeMetrics(existingScreen);
		}

		return existingScreen;
	}

	private Playlist getScreenshotPlaylist() {
		Playlist result = null;
		int pageWindow = ScreenDAO.getScreenshotInstances(true).size() * 2;

		// prioritize pages without any screenshot
		List<Page> pages = PageDAO.getNextPagesWithoutAutoScreenshot(pageWindow);
		if (pages.isEmpty()) {
			// renew already existing screenshots periodically
			pages = PageDAO.getNextPageForAutoScreenshot(pageWindow);
		}
		if (!pages.isEmpty()) {
			// TODO: pick a random page from the top X so that multiple Apple TVs can work
			// in
			// parallel, right now we cannot determine reliably if the screenshot being sent
			// fits to the page from the playlist if we activate random
			// Page page = pages.get(new Random().nextInt(pages.size()));
			Page page = pages.get(0);
			result = new Playlist("Automatic - Screenshot");
			PageReference pageRef = new PageReference(page);
			pageRef.setId(page.getId());
			result.addPageReference(pageRef);
		}
		return result;
	}

	private void removeMetrics(Screen result) {
		// remove metrics as they would otherwise invalidate the cache since they
		// constantly change
		result.setLastAlive(null);
		result.setLastContainerAlive(null);
		result.setAliveState(null);
		result.setRemoteUser(null);
		result.setDateModified(null);

		result.setMetric_browser(null);
		result.setMetric_currentPageId(0);
		result.setMetric_playlistTime(0);
		result.setMetric_os(null);
		result.setMetric_resX(0);
		result.setMetric_resY(0);
		result.setMetric_user(null);

		if (result.getPlaylist() != null) {
			removePlaylistMetrics(result.getPlaylist());
		}
	}

	private void checkRemoteValidity(Screen result) {
		if (result.getPageToShow() >= 0 && remoteControlKeys.getIfPresent(result.getId()) == null) {
			result.setPageToShow(-1);
			result.setRemoteUser(null);
			result = ScreenDAO.save(result);
		}
	}

	private void addOnboardingScreen(Screen screen) {
		screen.setName("Onboarding");

		String admins = ConfigUtil.getProperty(Consts.APP, Consts.PROP_ADMINISTRATORS);

		Page page1 = new Page("Onboarding in Progress");
		page1.setPageType(PageType.TEXT);
		page1.setText("Show your content right here! Visit <strong>" + getServerUrl()
				+ "</strong> and claim this screen with the key shown below.<p>Key: " + screen.getShortKey() + "</p>"
				+ ((StringUtils.isNotBlank(admins)) ? "<p>Admins: " + admins + "</p>" : ""));

		Playlist playlist = new Playlist();
		playlist.addPageReference(new PageReference(page1));

		screen.setPlaylist(playlist);
	}

	@POST
	@Path("/screens")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Screen createScreen(Screen screen) {
		screen.setName(sanitize(screen.getName()));

		if (StringUtils.isBlank(screen.getName())) {
			throwBadRequest("Name must be supplied.");
		}

		User user = userUtil.getLoggedInDBUser(request);
		Owner owner = new Owner(user);
		owner.setContact(true);
		screen.addOwner(owner);

		screen = ScreenDAO.saveNew(screen);
		if (screen.getPlaylist() != null) {
			screen.getPlaylist().expand();
		}

		return screen;
	}

	@PUT
	@Path("/screens/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen updateScreen(@PathParam("id") long screenId, Screen screenData) {
		Screen existingScreen = getExistingScreen(screenId, true);

		existingScreen.setName(screenData.getName());
		existingScreen.setPlaylist(screenData.getPlaylist());
		existingScreen.setScreenType(screenData.getScreenType());
		existingScreen.setShowOwners(screenData.isShowOwners());
		existingScreen.setShowHeader(screenData.isShowHeader());
		existingScreen.setShowFooter(screenData.isShowFooter());
		existingScreen.setLowMemoryMode(screenData.isLowMemoryMode());
		existingScreen.setScreenshotMode(screenData.isScreenshotMode());
		existingScreen.setTransitionMode(screenData.getTransitionMode());
		existingScreen = ScreenDAO.save(existingScreen);

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return existingScreen;
	}

	@PUT
	@Path("/screens/{id}/statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen updateScreenStatstics(@PathParam("id") long screenId, Screen screenData) {
		// update page statistics
		if (screenData.getPlaylist() != null && !screenData.isScreenshotMode()) {
			PageReference pageRef = new PageReferenceDAO().getById(screenData.getMetric_currentPageId());
			if (pageRef != null) {
				pageRef.setStats_viewCount(pageRef.getStats_viewCount() + 1);
				pageRef.setStats_showTime(
						pageRef.getStats_showTime() + screenData.getPlaylist().getPageDisplayDuration());
				pageRef = PageReferenceDAO.save(pageRef);

				Page existingPage = pageRef.getPage();
				if (existingPage != null) {
					existingPage.setStats_viewCount(existingPage.getStats_viewCount() + 1);
					existingPage.setStats_showTime(
							existingPage.getStats_showTime() + screenData.getPlaylist().getPageDisplayDuration());
					PageDAO.save(existingPage);
				}
			}
		}

		// update screen statistics
		Date eventDate = new Date();
		Screen existingScreen = new ScreenDAO().getById(screenId);

		existingScreen.setMetric_browser(screenData.getMetric_browser());
		existingScreen.setMetric_currentPageId(screenData.getMetric_currentPageId());
		existingScreen.setMetric_playlistTime(screenData.getMetric_playlistTime());
		existingScreen.setMetric_os(screenData.getMetric_os());
		existingScreen.setMetric_resX(screenData.getMetric_resX());
		existingScreen.setMetric_resY(screenData.getMetric_resY());
		existingScreen.setMetric_user(screenData.getMetric_user());
		existingScreen.setLastAlive(eventDate);

		existingScreen = ScreenDAO.save(existingScreen);
		EventHistoryDAO.save(new EventHistory(existingScreen.getId(), eventDate));

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return existingScreen;
	}

	@GET
	@Path("/screens/{id}/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen.AliveState getScreenHealth(@PathParam("id") long screenId) {
		Screen existingScreen = getExistingScreen(screenId, false);

		return existingScreen.getAliveState();
	}

	@GET
	@Path("/screens/{id}/events")
	@Produces(MediaType.APPLICATION_JSON)
	public List<EventHistory> getScreenEvents(@PathParam("id") long screenId) {
		return new EventHistoryDAO().getByField("entityId", screenId, "eventDate");
	}

	@GET
	@Path("/screens/{id}/eventsperhour")
	@Produces(MediaType.APPLICATION_JSON)
	public List<BasicNameValuePair> getScreenEventsPerHour(@PathParam("id") long screenId) {
		return calculateHistogram(calculateCount(getScreenEvents(screenId)), 24);
	}

	private Map<DateTime, Long> calculateCount(List<EventHistory> data) {
		Map<DateTime, Long> tempResult = new TreeMap<>();

		for (int i = 0; i < data.size(); i++) {
			Date date = data.get(i).getEventDate();
			DateTime hour = new DateTime(date);
			hour = hour.withTime(hour.getHourOfDay(), 0, 0, 0);

			if (tempResult.containsKey(hour)) {
				tempResult.put(hour, tempResult.get(hour) + 1);
			} else {
				tempResult.put(hour, 1L);
			}
		}

		return tempResult;
	}

	private List<BasicNameValuePair> calculateHistogram(Map<DateTime, Long> data, int hourCount) {
		List<BasicNameValuePair> result = new ArrayList<>();
		DateTime curDate = new DateTime();
		curDate = curDate.withTime(curDate.getHourOfDay(), 0, 0, 0);

		for (int i = 0; i < hourCount; i++) {
			BasicNameValuePair entry;
			if (data.containsKey(curDate)) {
				entry = new BasicNameValuePair(curDate.toString(), data.get(curDate).toString());
			} else {
				entry = new BasicNameValuePair(curDate.toString(), "0");
			}
			result.add(entry);
			curDate = curDate.minusHours(1);
		}

		return Lists.reverse(result);
	}

	@GET
	@Path("/screens/{id}/control")
	@Produces(MediaType.APPLICATION_JSON)
	public Integer getRemoteKey(@PathParam("id") long screenId) {
		int key = 1 + new Random().nextInt(9999);
		remoteControlKeys.put(screenId, key);

		return key;
	}

	@PUT
	@Path("/screens/{id}/control/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen remoteControlScreen(@PathParam("id") long screenId, Screen screenData, @PathParam("key") int key) {
		if (key <= 0) {
			throwBadRequest();
		}
		if (remoteControlKeys.getIfPresent(screenId) != key) {
			throwUnauthorized();
		}

		User user = userUtil.getLoggedInDBUser(request);

		Screen existingScreen = new ScreenDAO().getById(screenId);
		existingScreen.setPageToShow(screenData.getPageToShow());
		existingScreen.setRemoteUser(screenData.getPageToShow() < 0 ? null : user.getUserId());
		existingScreen = ScreenDAO.save(existingScreen);

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return existingScreen;
	}

	@POST
	@Path("/screens/{id}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	public Screen copyScreen(@PathParam("id") long screenId, Screen newScreen) {
		Screen existingScreen = getExistingScreen(screenId, false);

		Screen copiedScreen = new Screen(existingScreen);
		copiedScreen.setName(newScreen.getName());
		copiedScreen = createScreen(copiedScreen);

		return copiedScreen;
	}

	@PUT
	@Path("/screens/{id}/requestedOwner")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen updateRequestedOwner(@PathParam("id") long screenId, Screen screenData) {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		Screen existingScreen = new ScreenDAO().getById(screenId);
		if (!existingScreen.getOwners().isEmpty()) {
			throwBadRequest();
		}

		existingScreen.setRequestedOwner(screenData.getRequestedOwner());
		existingScreen = ScreenDAO.save(existingScreen);

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return existingScreen;
	}

	@POST
	@Path("/screens/{id}/onboard")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen onboardScreen(@PathParam("id") long screenId) {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		Screen existingScreen = new ScreenDAO().getById(screenId);
		if (!existingScreen.getOwners().isEmpty()) {
			throwBadRequest();
		}

		User user = userUtil.getDBUser(existingScreen.getRequestedOwner());
		Owner owner = new Owner(user);
		owner.setContact(true);
		existingScreen.addOwner(owner);
		existingScreen.setName(existingScreen.getKey());
		existingScreen.setRequestedOwner(null);

		return ScreenDAO.save(existingScreen);
	}

	@POST
	@Path("/claim/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen claimScreen(@PathParam("key") String key, @DefaultValue("0") @QueryParam("screenId") long screenId) {
		List<Screen> onboardings = ScreenDAO.getOnboardings();
		Screen existingScreen = null;
		for (Screen screen : onboardings) {
			if (key.equalsIgnoreCase(screen.getShortKey())) {
				existingScreen = screen;
				break;
			}
		}
		if (existingScreen == null) {
			throwNotFound();
		}
		if (!existingScreen.getOwners().isEmpty()) {
			throwBadRequest("Screen is already onboarded. Contact the administrators if you think this is incorrect.");
		}

		if (screenId > 0) {
			// reuse existing screen
			Screen reuseScreen = new ScreenDAO().getById(screenId);
			if (reuseScreen == null) {
				throwNotFound();
			}
			if (!this.isAdmin() && !inAdminOwners(reuseScreen.getOwners())) {
				throwUnauthorized();
			}
			reuseScreen.setKey(existingScreen.getKey());

			// delete onboarding screen as it is not needed anymore
			new ScreenDAO().deleteById(existingScreen.getId());

			return ScreenDAO.save(reuseScreen);
		} else {
			// create new screen
			User user = userUtil.getLoggedInDBUser(request);
			Owner owner = new Owner(user);
			owner.setContact(true);
			existingScreen.addOwner(owner);
			existingScreen.setName(user.getFullName() + "'s Screen");
			return ScreenDAO.save(existingScreen);
		}
	}

	@DELETE
	@Path("/screens/{screenId}")
	public Response deleteScreen(@PathParam("screenId") long screenId) {
		Screen existingScreen = getExistingScreen(screenId, true);

		for (Owner owner : existingScreen.getOwners()) {
			new OwnerDAO().deleteById(owner.getId());
		}

		return new ScreenDAO().deleteById(screenId) ? RESPONSE_OK : RESPONSE_BAD;
	}

	@Override
	@POST
	@Path("/screens/{screenId}/owners")
	@Consumes(MediaType.APPLICATION_JSON)
	public Screen addOwners(@PathParam("screenId") long screenId, Owner[] ownerData) {
		Screen existingScreen = getExistingScreen(screenId, true);
		doAddOwners(existingScreen, ownerData);

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return ScreenDAO.save(existingScreen);
	}

	@Override
	@DELETE
	@Path("/screens/{screenId}/owners/{ownerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Screen deleteOwner(@PathParam("screenId") long screenId, @PathParam("ownerId") long ownerId) {
		Screen existingScreen = getExistingScreen(screenId, true);
		doDeleteOwner(existingScreen, ownerId);
		existingScreen = ScreenDAO.save(existingScreen);

		if (existingScreen.getPlaylist() != null) {
			existingScreen.getPlaylist().expand();
		}

		return existingScreen;
	}

	@POST
	@Path("/screenkey/{key}/appliancestart")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerApplianceStart(@PathParam("key") String key, ApplianceInfo logContent) {
		Screen existingScreen = ScreenDAO.getScreenByKey(key);
		if (existingScreen == null) {
			throwNotFound();
		}

		existingScreen.setMetric_applianceStarts(existingScreen.getMetric_applianceStarts() + 1);

		if (logContent != null) {
			logger.info("Appliance Version: " + logContent.getVersion());

			existingScreen.setMetric_applianceStartDate(logContent.getTimestamp());
			existingScreen.setMetric_applianceVersion(logContent.getVersion());

			if (logContent.getDump() != null) {
				String newContent = logContent.getDump().getTimestamp() + " (" + logContent.getDump().getVersion()
						+ "): " + logContent.getDump().getException() + " " + logContent.getDump().getReason() + "\n"
						+ logContent.getDump().getStacktrace();

				logger.info("Appliance Log: " + newContent);

				if (!StringUtils.isAllBlank(existingScreen.getMetric_applianceLog())) {
					newContent += "\n\n--------------------\n\n" + existingScreen.getMetric_applianceLog();
				}
				if (newContent.length() > 4000) {
					newContent = newContent.substring(0, 4000);
				}
				existingScreen.setMetric_applianceLog(newContent);
			}
		}
		ScreenDAO.save(existingScreen);

		return RESPONSE_OK;
	}

}
