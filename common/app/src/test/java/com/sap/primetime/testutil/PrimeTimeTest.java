package com.sap.primetime.testutil;

import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletContext;

import org.joda.time.DateTimeZone;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.sap.primetime.dao.ConfigDAO;
import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PageReferenceDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.startup.AppInitializer;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigUtil;

/**
 * Utility class providing commonly used test helper functions and mocks.
 *
 */
public abstract class PrimeTimeTest {
	protected User simpleUser = new User("c1", "testuser@test.com");
	protected User simpleUser2 = new User("c2", "testuser2@test.com");
	protected HttpServletRequestMock requestMock;
	protected ServletContext contextMock;

	public void prepareTest() throws Exception {
		ConfigUtil.setEnumeratePropertyFiles(false);
		ConfigUtil.setRuntimeProperty(Consts.APP, Consts.PROP_FILESUPPORT, "true");
		ConfigUtil.setRuntimeProperty(Consts.APP, Consts.PROP_DEVELOPMENTURL, "https://test");

		Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new TestModule());
		Consts.injector = injector;

		deleteAllDBEntries();

		System.getProperties().setProperty("net.fortuna.ical4j.timezone.update.enabled", "false");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		DateTimeZone.setDefault(DateTimeZone.UTC);

		AppInitializer.initConfiguration();
		ConfigUtil.setRuntimeProperty(Consts.APP, Consts.PROP_AVATARURL,
				"https://avatars.services.sap.com/images/%firstName%.%lastName%_small.png");

		requestMock = spy(new HttpServletRequestMock(simpleUser));
		contextMock = new ServletContextMock();
	}

	protected Owner findOwner(String userId, List<Owner> owners) {
		for (Owner owner : owners) {
			if (owner.getUser().getUserId().equals(userId)) {
				return owner;
			}
		}

		return null;
	}

	private void deleteAllDBEntries() {
		// this order is intentionally since there are some dependencies which are not
		// managed by JPA currently
		new EventHistoryDAO().deleteAll();
		new ConfigDAO().deleteAll();
		new OwnerDAO().deleteAll();
		new PageReferenceDAO().deleteAll();
		new PageDAO().deleteAll();
		new FileDAO().deleteAll();
		new ScreenDAO().deleteAll();
		new PlaylistDAO().deleteAll();
		new UserDAO().deleteAll();
	}

}