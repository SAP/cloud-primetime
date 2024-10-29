package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.ApplianceError;
import com.sap.primetime.dao.dto.ApplianceInfo;
import com.sap.primetime.dao.entities.EventHistory;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.HttpServletRequestMock;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.testutil.UploadHelper;

public class ScreenServiceTest extends PrimeTimeTest {
	private PageService pageService;
	private PlaylistService playlistService;
	private ScreenService screenService;
	private FileService fileService;
	private Playlist playlist;

	@Before
	public void setup() throws Exception {
		prepareTest();

		pageService = new PageService();
		pageService.request = requestMock;

		playlistService = new PlaylistService();
		playlistService.request = requestMock;

		fileService = new FileService();
		fileService.request = requestMock;

		screenService = spy(new ScreenService());
		screenService.request = requestMock;

		// reusable test data
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));
		playlist = playlistService.createPlaylist(new Playlist("d1"));
		playlist = playlistService.addPage(playlist.getId(), page1);
		playlist = playlistService.addPage(playlist.getId(), page2);
	}

	@Test
	public void testGetTestScreen() throws Exception {
		assertEquals(5, screenService.getScreen(0, false, false).getPlaylist().getPageReferences().size());
	}

	@Test
	public void testCreateScreen() throws Exception {
		assertEquals(0, getScreens().size());

		screenService.createScreen(new Screen("s1", playlist));
		List<Screen> screens = getScreens();
		assertEquals(1, screens.size());
		assertEquals("s1", screens.get(0).getName());
		assertNotNull(screens.get(0).getPlaylist());
		assertEquals(1, screens.get(0).getOwners().size());
		assertEquals(simpleUser.getUserId(), screens.get(0).getOwners().get(0).getUser().getUserId());

		screenService.createScreen(new Screen("s2", playlist));
		assertEquals(2, getScreens().size());
	}

	@Test
	public void testGetScreensForUser() throws Exception {
		screenService.createScreen(new Screen("s1", playlist));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		screenService.request = requestMock;

		screenService.createScreen(new Screen("s2", playlist));
		List<Screen> screens = screenService.getScreens(simpleUser2.getUserId());
		assertEquals(1, screens.size());
		assertEquals("s2", screens.get(0).getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetScreensForOtherUser() throws Exception {
		screenService.createScreen(new Screen("s1", playlist));
		screenService.getScreens(simpleUser2.getUserId());
	}

	@Test
	public void testCopyScreen() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1"));
		screen = screenService.copyScreen(screen.getId(), new Screen("s2"));

		List<Screen> screens = getScreens();
		assertEquals(2, screens.size());
		assertEquals("s2", screens.get(1).getName());
		assertEquals(screen.getId(), screens.get(1).getId());
	}

	@Test(expected = WebApplicationException.class)
	public void testCopyNonExistentScreens() throws Exception {
		screenService.copyScreen(-1, new Screen());
	}

	@Test
	public void testStatistics() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));
		assertEquals(0, new EventHistoryDAO().getCount());

		Screen screenData = new Screen();
		screenData.setMetric_browser("browser");
		screenData.setMetric_currentPageId(playlist.getPageReferences().get(0).getId());
		screenData.setPlaylist(playlist);
		screen = screenService.updateScreenStatstics(screen.getId(), screenData);

		assertEquals(1, new EventHistoryDAO().getCount());
		EventHistory event = new EventHistoryDAO().getAll().get(0);
		assertEquals(EventHistory.EventType.ScreenAlive, event.getEventType());
		assertEquals(screen.getId(), event.getEntityId());
		assertNotNull(event.getEventDate());

		// ensure metrics are stripped
		screen = screenService.getScreen(screen.getId(), false, false);
		assertNull(screen.getLastAlive());
		assertNull(screen.getMetric_browser());
		assertEquals(0, screen.getPlaylist().getPageReferences().get(0).getStats_viewCount());
		assertEquals(0, screen.getPlaylist().getPageReferences().get(0).getStats_showTime());
		assertEquals(0, screen.getPlaylist().getPageReferences().get(0).getPage().getStats_viewCount());
		assertEquals(0, screen.getPlaylist().getPageReferences().get(0).getPage().getStats_showTime());

		// ensure statistics go up
		screen = screenService.getScreen(screen.getId(), true, false);
		assertNotNull(screen.getLastAlive());
		assertEquals("browser", screen.getMetric_browser());
		assertEquals(1, screen.getPlaylist().getPageReferences().get(0).getStats_viewCount());
		assertEquals(playlist.getPageDisplayDuration(),
				screen.getPlaylist().getPageReferences().get(0).getStats_showTime());
		assertEquals(1, screen.getPlaylist().getPageReferences().get(0).getPage().getStats_viewCount());
		assertEquals(playlist.getPageDisplayDuration(),
				screen.getPlaylist().getPageReferences().get(0).getPage().getStats_showTime());
	}

	@Test
	public void testEventHistory() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));

		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 0, 0, 0).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 0, 0, 15).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 0, 0, 30).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 0, 40, 0).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 1, 0, 0).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 2, 0, 0).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 5, 0, 0).toDate()));
		EventHistoryDAO.saveNew(new EventHistory(screen.getId(), new DateTime(2018, 1, 1, 5, 0, 15).toDate()));
		// other screen to check for correct selection
		EventHistoryDAO.saveNew(new EventHistory(-1, new DateTime(2018, 1, 1, 0, 0, 0).toDate()));

		List<EventHistory> events = screenService.getScreenEvents(screen.getId());
		assertEquals(8, events.size());
		assertEquals(EventHistory.EventType.ScreenAlive, events.get(0).getEventType());
	}

	@Test
	public void testUpdateScreen() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));
		screenService.updateScreen(screen.getId(), new Screen("s2"));
		assertEquals("s2", screenService.getScreen(screen.getId(), true, false).getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingScreen() throws Exception {
		assertNull(screenService.getScreen(-1, true, false));
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingScreenByKey() throws Exception {
		assertNull(screenService.getScreenByKey("nonexisting", true, false, false, null));
	}

	@Test
	public void testDeleteScreen() throws Exception {
		screenService.createScreen(new Screen("s1", playlist));
		assertEquals(1, getScreens().size());
		screenService.deleteScreen(getScreens().get(0).getId());
		assertEquals(0, getScreens().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testDeleteNonExistingScreen() throws Exception {
		screenService.createScreen(new Screen("s1", playlist));
		screenService.deleteScreen(-1);
	}

	@Test
	public void testAddOwners() throws Exception {
		Screen screen = new Screen("s1");
		screen = screenService.createScreen(screen);
		assertEquals(1, screenService.getScreen(screen.getId(), true, false).getOwners().size());

		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(2, screenService.getScreen(screen.getId(), true, false).getOwners().size());
		List<Owner> owners = screenService.getScreen(screen.getId(), true, false).getOwners();
		assertEquals("i1", owners.get(1).getUser().getUserId());

		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i2")) });
		assertEquals(3, screenService.getScreen(screen.getId(), true, false).getOwners().size());
	}

	@Test
	public void testNoOwner() throws Exception {
		Screen screen = new Screen("s1");
		screen = screenService.createScreen(screen);
		assertEquals(1, getScreens().size());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		screenService.request = requestMock;
		assertEquals(0, getScreens().size());

		// switch user to add owner
		requestMock = new HttpServletRequestMock(simpleUser);
		screenService.request = requestMock;
		screenService.addOwners(screen.getId(), new Owner[] { new Owner(simpleUser2) });

		// switch back
		requestMock = new HttpServletRequestMock(simpleUser2);
		screenService.request = requestMock;
		assertEquals(1, getScreens().size());
	}

	@Test(expected = WebApplicationException.class)
	@Ignore
	public void testAddDuplicateOwner() throws Exception {
		Screen screen = new Screen("s1");
		screenService.createScreen(screen);
		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner() throws Exception {
		Screen screen = new Screen("s1");
		screen = screenService.createScreen(screen);
		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("wrongUserId")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner2() throws Exception {
		Screen screen = new Screen("s1");
		screen = screenService.createScreen(screen);
		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1i2")) });
	}

	@Test
	public void testDeleteOwner() throws Exception {
		Screen screen = new Screen("s1");
		screen = screenService.createScreen(screen);
		screen = screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
		screen = screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i2")) });

		assertEquals(3, screenService.getScreen(screen.getId(), true, false).getOwners().size());
		screenService.deleteOwner(screen.getId(), screen.getOwners().get(1).getId());

		assertEquals(2, screenService.getScreen(screen.getId(), true, false).getOwners().size());
		assertEquals("i2",
				screenService.getScreen(screen.getId(), true, false).getOwners().get(1).getUser().getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdateScreenByNonOwner() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		screenService.request = requestMock;

		screenService.updateScreen(screen.getId(), new Screen("s2"));
	}

	@Test(expected = WebApplicationException.class)
	public void testUnauthorizedOnboarding() throws Exception {
		screenService.getOnboardings();
	}

	@Test(expected = WebApplicationException.class)
	public void testUnauthorizedOnboarding2() throws Exception {
		Screen screen = screenService.getScreenByKey("key1", false, false, true, null);
		screenService.onboardScreen(screen.getId());
	}

	@Test(expected = WebApplicationException.class)
	public void testUnreachableNotOnboardedScreen() throws Exception {
		Screen screen = screenService.getScreenByKey("key1", false, false, true, null);
		screenService.getScreen(screen.getId(), false, false);
	}

	@Test(expected = WebApplicationException.class)
	public void testOnboardOnboardedScreen() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1"));
		screenService.onboardScreen(screen.getId());
	}

	@Test
	public void testScreenOnboarding() throws Exception {
		assertNotNull(screenService.getScreenByKey("key1", false, false, true, null));
		assertEquals(1, new ScreenDAO().getCount());

		Screen screen = screenService.getScreenByKey("key1", false, false, true, null);
		assertNotNull(screen);
		assertEquals(1, new ScreenDAO().getCount());
		assertEquals(1, screen.getPlaylist().getPageReferences().size());
		assertEquals(simpleUser.getUserId(), screen.getRequestedOwner());
		assertTrue(screen.getId() > 0);

		// simulate user with admin priviledges
		doReturn(true).when(screenService).isAdmin();

		assertEquals(1, screenService.getOnboardings().size());
		assertEquals(0, screenService.getScreen(screen.getId(), false, false).getOwners().size());
		screenService.getScreenByKey("key2", false, false, true, null);
		assertEquals(2, screenService.getOnboardings().size());

		screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(1, screenService.getOnboardings().size());

		screen = screenService.onboardScreen(screenService.getOnboardings().get(0).getId());
		assertEquals(1, screen.getOwners().size());
		assertEquals(screen.getKey(), screen.getName());
		assertNull(screen.getRequestedOwner());
		assertEquals(simpleUser.getUserId(), screen.getOwners().get(0).getUser().getUserId());
		assertEquals(0, screenService.getOnboardings().size());
	}

	@Test
	public void testClaimScreen() throws Exception {
		Screen screen = screenService.getScreenByKey("KEY1-and-a-lot-of-digits", false, false, true, null);
		screen = screenService.claimScreen("key1", 0);

		assertEquals(1, screen.getOwners().size());
		assertEquals(simpleUser.getUserId(), screen.getOwners().get(0).getUser().getUserId());
		assertTrue(screen.getName().length() > 0);
	}

	@Test(expected = WebApplicationException.class)
	public void testClaimTakenScreen() throws Exception {
		screenService.getScreenByKey("key1", false, false, true, null);
		screenService.claimScreen("key1", 0);
		screenService.claimScreen("key1", 0);
	}

	@Test
	public void testClaimScreenToExisting() throws Exception {
		Screen screen1 = new Screen("s1");
		screen1 = screenService.createScreen(screen1);

		Screen screen2 = screenService.getScreenByKey("key1", false, false, true, null);
		assertEquals(2, new ScreenDAO().getCount());

		screen2 = screenService.claimScreen("key1", screen1.getId());

		assertEquals(screen1.getId(), screen2.getId());
		assertEquals(1, new ScreenDAO().getCount());
		assertEquals(1, screen2.getOwners().size());
	}

	@Test
	public void testUpdateRequestedOwner() throws Exception {
		Screen screen = screenService.getScreenByKey("key1", false, false, true, null);

		// simulate user with admin priviledges
		doReturn(true).when(screenService).isAdmin();

		Screen screenUpdate = new Screen();
		screenUpdate.setRequestedOwner("newowner");
		screen = screenService.updateRequestedOwner(screen.getId(), screenUpdate);
		assertEquals("newowner", screen.getRequestedOwner());

		screen = screenService.onboardScreen(screenService.getOnboardings().get(0).getId());
		assertEquals("newowner", screen.getOwners().get(0).getUser().getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testUnauthorizedOwnerUpdate() throws Exception {
		Screen screen = screenService.getScreenByKey("key1", false, false, true, null);
		screenService.updateRequestedOwner(screen.getId(), new Screen());
	}

	@Test(expected = WebApplicationException.class)
	public void testControlNoAuth() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));
		Screen screenUpdate = new Screen();
		screenUpdate.setPageToShow(2);
		screenService.remoteControlScreen(screen.getId(), screenUpdate, 0);
	}

	@Test
	public void testControl() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));
		assertEquals(-1, screen.getPageToShow());
		assertEquals(null, screen.getRemoteUser());

		// simulate page load and activate control
		screen = screenService.getScreen(screen.getId(), false, false);
		int key = screenService.getRemoteKey(screen.getId());
		assertTrue(key > 0);

		Screen screenUpdate = new Screen();
		screenUpdate.setPageToShow(2);
		screen = screenService.remoteControlScreen(screen.getId(), screenUpdate, key);
		assertEquals(2, screen.getPageToShow());
		assertEquals(simpleUser.getUserId(), screen.getRemoteUser());

		// deactivate control
		screenUpdate.setPageToShow(-1);
		screen = screenService.remoteControlScreen(screen.getId(), screenUpdate, key);
		assertEquals(-1, screen.getPageToShow());
		assertEquals(null, screen.getRemoteUser());
	}

	@Test(expected = WebApplicationException.class)
	public void testConcurrentControl() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1", playlist));

		// simulate page load and activate control
		screen = screenService.getScreen(screen.getId(), false, false);
		int key1 = screenService.getRemoteKey(screen.getId());
		int key2 = screenService.getRemoteKey(screen.getId());
		assertTrue(key1 > 0);
		assertTrue(key2 > 0);
		assertNotEquals(key1, key2);

		screenService.remoteControlScreen(screen.getId(), new Screen(), key1);
	}

	@Test()
	public void testApplianceStarts() throws Exception {
		Screen screen = new Screen("s1", playlist);
		screen.setKey("k1");
		screen = screenService.createScreen(screen);
		assertEquals(0, screen.getMetric_applianceStarts());
		assertNull(screen.getMetric_applianceLog());

		screenService.registerApplianceStart(screen.getKey(), null);
		screen = screenService.getScreen(screen.getId(), true, false);
		assertEquals(1, screen.getMetric_applianceStarts());
		assertNull(screen.getMetric_applianceLog());

		screenService.registerApplianceStart(screen.getKey(), new ApplianceInfo(
				new ApplianceError("stacktrace1", new Date(), "exception1", "reason1", "1.0.0"), null, "1.1.0"));
		screen = screenService.getScreen(screen.getId(), true, false);
		assertEquals(2, screen.getMetric_applianceStarts());
		assertNotNull(screen.getMetric_applianceLog());
		assertTrue(screen.getMetric_applianceLog().contains("reason1"));
		assertFalse(screen.getMetric_applianceLog().contains("null"));

		// check for correct truncation
		screenService.registerApplianceStart(screen.getKey(), new ApplianceInfo(new ApplianceError("stacktrace2",
				new Date(), "exception2", StringUtils.repeat("reason2", 4000), "1.0.0"), null, "1.1.0"));
		screen = screenService.getScreen(screen.getId(), true, false);
		assertEquals(4000, screen.getMetric_applianceLog().length());
		assertFalse(screen.getMetric_applianceLog().contains("reason1"));
	}

	@Test()
	public void testApplianceAsSlave() throws Exception {
		// start without pages in the system
		pageService.deletePage(playlist.getPageReferences().get(0).getPage().getId());
		pageService.deletePage(playlist.getPageReferences().get(1).getPage().getId());

		Screen screen = new Screen("s1");
		screen.setKey("k1");
		screen = screenService.createScreen(screen);
		assertEquals(0, ScreenDAO.getScreenshotInstances(false).size());

		// usually there should be no playlist
		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertNull(screen.getPlaylist());

		// in screenshot mode a playlist should be created dynamically
		screen.setScreenshotMode(true);
		screen = screenService.updateScreen(screen.getId(), screen);
		assertEquals(1, ScreenDAO.getScreenshotInstances(false).size());

		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertNull(screen.getPlaylist());

		// create some pages
		Page page1 = new Page("p1");
		page1.setPageType(PageType.TEXT);
		page1 = pageService.createPage(page1);

		Page page2 = new Page("p2");
		page2.setPageType(PageType.PDF);
		page2 = pageService.createPage(page2);

		Page page3 = new Page("p3");
		page3.setPageType(PageType.URL);
		page3 = pageService.createPage(page3);

		// now always one page should be returned
		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertNotNull(screen.getPlaylist());
		assertEquals(1, screen.getPlaylist().getPageReferences().size());
		assertTrue(screen.getPlaylist().getPageReferences().get(0).getId() > 0);
		assertNull(screen.getPlaylist().getPageReferences().get(0).getPage().getScreenshot());
		assertNull(screen.getPlaylist().getDateCreated());
		assertNull(screen.getPlaylist().getDateModified());
		assertEquals(2, PageDAO.getNextPagesWithoutAutoScreenshot(10).size());

		// simulate Apple TV sending screenshot
		Screen screenData = new Screen();
		screenData.setMetric_currentPageId(screen.getPlaylist().getPageReferences().get(0).getId());
		screenData.setPlaylist(screen.getPlaylist());
		screenService.updateScreenStatstics(screen.getId(), screenData);

		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("k1");

		Page testPage1 = pageService.getPage(screen.getPlaylist().getPageReferences().get(0).getPage().getId());
		assertNull(testPage1.getAutoScreenshot());

		// advance time since playlist needs to be shown for a while
		screenData.setMetric_playlistTime(31);
		screenService.updateScreenStatstics(screen.getId(), screenData);
		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("k1");

		testPage1 = pageService.getPage(screen.getPlaylist().getPageReferences().get(0).getPage().getId());
		assertNotNull(testPage1.getAutoScreenshot());
		assertEquals(1, PageDAO.getNextPagesWithoutAutoScreenshot(10).size());

		// simulate next page
		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertNotNull(screen.getPlaylist());

		screenData = new Screen();
		screenData.setMetric_currentPageId(screen.getPlaylist().getPageReferences().get(0).getId());
		screenData.setMetric_playlistTime(31);
		screenData.setPlaylist(screen.getPlaylist());
		screenService.updateScreenStatstics(screen.getId(), screenData);

		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("k1");

		Page testPage2 = pageService.getPage(screen.getPlaylist().getPageReferences().get(0).getPage().getId());
		assertNotNull(testPage2.getAutoScreenshot());
		assertNotEquals(testPage1.getAutoScreenshot().getId(), testPage2.getAutoScreenshot().getId());
		assertEquals(0, PageDAO.getNextPagesWithoutAutoScreenshot(10).size());

		// now maintenance cycle starts since all pages have an auto-screenshot
		assertEquals(2, PageDAO.getNextPageForAutoScreenshot(10).size());
		assertEquals("p1", PageDAO.getNextPageForAutoScreenshot(10).get(0).getName());

		// simulate next page
		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertNotNull(screen.getPlaylist());

		screenData = new Screen();
		screenData.setMetric_currentPageId(screen.getPlaylist().getPageReferences().get(0).getId());
		screenData.setMetric_playlistTime(31);
		screenData.setPlaylist(screen.getPlaylist());
		screenService.updateScreenStatstics(screen.getId(), screenData);

		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("k1");

		screen = screenService.getScreenByKey("k1", true, true, false, null);
		assertEquals(2, PageDAO.getNextPageForAutoScreenshot(10).size());
		assertEquals("p3", PageDAO.getNextPageForAutoScreenshot(10).get(0).getName());
	}

	private List<Screen> getScreens() {
		return screenService.getScreens(null);
	}
}