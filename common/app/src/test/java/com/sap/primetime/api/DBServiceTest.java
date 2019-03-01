package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;

import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.util.UserUtil;

public class DBServiceTest extends PrimeTimeTest {
	private DBService dbService;
	private PlaylistService playlistService;
	private PageService pageService;
	private ScreenService screenService;
	private UserService userService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		dbService = spy(new DBService());
		dbService.request = requestMock;

		playlistService = new PlaylistService();
		playlistService.request = requestMock;

		pageService = new PageService();
		pageService.request = requestMock;

		screenService = new ScreenService();
		screenService.request = requestMock;

		userService = spy(new UserService());
		userService.request = requestMock;
	}

	@Test(expected = WebApplicationException.class)
	public void testUnauthorizedDBAdminUsers() throws Exception {
		dbService.getUsers();
	}

	@Test
	public void testGetUsers() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();

		assertEquals(0, dbService.getUsers().size());
		UserUtil.getInstance().getDBUser("u1");
		assertEquals(1, dbService.getUsers().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testDeleteUnknownUser() throws Exception {
		userService.deleteUser(-1);
	}

	@Test
	public void testDeleteUser() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();
		doReturn(true).when(userService).isDBAdmin();

		User user = UserUtil.getInstance().getDBUser("u1");
		userService.deleteUser(user.getId());
		assertEquals(0, dbService.getUsers().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testDeleteUserWithoutAdminPermission() throws Exception {
		User user = UserUtil.getInstance().getDBUser("u1");
		userService.deleteUser(user.getId());
	}

	@Test
	public void testDeleteUser2() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();
		doReturn(true).when(userService).isDBAdmin();

		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(2, dbService.getUsers().size());
		assertEquals(2, playlistService.getPlaylist(playlist.getId(), false, false, true).getOwners().size());

		userService.deleteUser(UserDAO.getUserByUserId("i1").getId());
		assertEquals(1, dbService.getUsers().size());
		assertEquals(1, playlistService.getPlaylist(playlist.getId(), false, false, true).getOwners().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testUnauthorizedDBAdminPages() throws Exception {
		dbService.getPages();
	}

	@Test
	public void testGetPages() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();

		assertEquals(0, dbService.getPages().size());
		pageService.createPage(new Page("p1"));
		assertEquals(1, dbService.getPages().size());
	}

	@Test
	public void testGetFiles() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();

		assertEquals(0, dbService.getFiles().size());
		FileDAO.saveNew((new File("f1")));
		assertEquals(1, dbService.getFiles().size());
	}

	@Test
	public void testGetPlaylists() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();

		assertEquals(0, dbService.getPlaylists().size());
		playlistService.createPlaylist(new Playlist("d1"));
		assertEquals(1, dbService.getPlaylists().size());
	}

	@Test
	public void testGetScreens() throws Exception {
		// simulate user with dbadmin priviledges
		doReturn(true).when(dbService).isDBAdmin();

		assertEquals(0, dbService.getScreens().size());
		screenService.createScreen(new Screen("s1"));
		assertEquals(1, dbService.getScreens().size());
	}

}