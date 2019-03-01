package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;

import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PageReferenceDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.PlaylistReferences;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.PageReference.PageRefType;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.HttpServletRequestMock;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.testutil.UploadHelper;

public class PlaylistServiceTest extends PrimeTimeTest {
	private PageService pageService;
	private PlaylistService playlistService;
	private ScreenService screenService;
	private FileService fileService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		pageService = new PageService();
		pageService.request = requestMock;

		playlistService = new PlaylistService();
		playlistService.request = requestMock;

		screenService = new ScreenService();
		screenService.request = requestMock;

		fileService = new FileService();
		fileService.request = requestMock;
	}

	@Test
	public void testCreatePlaylists() throws Exception {
		assertEquals(0, getPlaylists(false).size());

		playlistService.createPlaylist(new Playlist("d1"));
		List<Playlist> playlists = getPlaylists(false);
		assertEquals(1, playlists.size());
		assertEquals(1, playlists.get(0).getOwners().size());
		assertEquals(simpleUser.getUserId(), playlists.get(0).getOwners().get(0).getUser().getUserId());

		playlistService.createPlaylist(new Playlist("d2"));
		assertEquals(2, getPlaylists(false).size());
	}

	@Test
	public void testGetPlaylistsForUser() throws Exception {
		playlistService.createPlaylist(new Playlist("d1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		playlistService.request = requestMock;

		playlistService.createPlaylist(new Playlist("d2"));
		List<Playlist> playlists = playlistService.getPlaylists(false, simpleUser2.getUserId());
		assertEquals(1, playlists.size());
		assertEquals("d2", playlists.get(0).getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetPlaylistsForOtherUser() throws Exception {
		playlistService.createPlaylist(new Playlist("d1"));
		playlistService.getPlaylists(false, simpleUser2.getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingPlaylist() throws Exception {
		assertNull(getPlaylist(-1));
	}

	@Test
	public void testGetPlaylist() throws Exception {
		Playlist db = playlistService.createPlaylist(new Playlist("d1"));

		assertNotNull(getPlaylist(db.getId()));
		assertEquals(0, getPlaylist(db.getId()).getPageReferences().size());
	}

	@Test
	public void testUpdatePlaylist() throws Exception {
		Playlist db = playlistService.createPlaylist(new Playlist("d1"));
		playlistService.updatePlaylist(db.getId(), new Playlist("d2"));
		assertEquals("d2", getPlaylist(db.getId()).getName());
	}

	@Test
	public void testDeletePlaylist() throws Exception {
		playlistService.createPlaylist(new Playlist("d1"));
		assertEquals(1, getPlaylists(false).size());
		playlistService.deletePlaylist(getPlaylists(false).get(0).getId());
		assertEquals(0, getPlaylists(false).size());
	}

	@Test
	public void testDeletePlaylistWithPages() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));

		Playlist playlist = playlistService.createPlaylist(new Playlist("d1"));
		playlistService.addPage(playlist.getId(), page1);
		assertEquals(1, getPlaylists(false).size());

		playlistService.deletePlaylist(playlist.getId());
		assertEquals(0, getPlaylists(false).size());
		assertEquals(1, new PageDAO().getAll().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testDeleteNonExistingPlaylist() throws Exception {
		playlistService.createPlaylist(new Playlist("d1"));
		playlistService.deletePlaylist(-1);
	}

	@Test(expected = WebApplicationException.class)
	public void testDeletePlaylistUsedInScreen() throws Exception {
		Playlist playlist1 = playlistService.createPlaylist(new Playlist("d1"));

		Screen screen = new Screen("s1", playlist1);
		screen = screenService.createScreen(screen);

		playlistService.deletePlaylist(playlist1.getId());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddNonPersistedPage() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlistService.addPage(playlist.getId(), new Page("p1"));
	}

	@Test
	public void testAddPages() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		assertEquals(0, getPlaylist(playlist.getId()).getPageReferences().size());

		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		playlistService.addPage(playlist.getId(), page1);
		playlistService.addPage(playlist.getId(), page2);
		List<PageReference> pageReferences = getPlaylist(playlist.getId()).getPageReferences();
		assertEquals("p1", pageReferences.get(0).getPage().getName());
		assertEquals("p2", pageReferences.get(1).getPage().getName());
		assertEquals(0, pageReferences.get(0).getPageDisplayDurationOverride());
	}

	@Test
	public void testMoviePage() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);

		UploadHelper.simulateFileUpload(requestMock, "small.mp4", "mp4");
		fileService.uploadFile();

		Page page1 = pageService.createPage(new Page("p1"));
		page1.setFile(fileService.getFiles(null).get(0));
		page1.setPageType(PageType.MOVIE);
		page1 = PageDAO.save(page1);

		playlistService.addPage(playlist.getId(), page1);
		List<PageReference> pageReferences = getPlaylist(playlist.getId()).getPageReferences();
		assertEquals(5, pageReferences.get(0).getPageDisplayDurationOverride());
	}

	@Test
	public void testPageVisibility() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);

		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		playlistService.addPage(playlist.getId(), page1);
		playlistService.addPage(playlist.getId(), page2);
		List<PageReference> pageReferences = playlistService.getPlaylist(playlist.getId(), true, false, true)
				.getPageReferences();
		assertEquals(2, pageReferences.size());

		pageReferences.get(0).setVisible(false);
		PageReferenceDAO.save(pageReferences.get(0));

		pageReferences = playlistService.getPlaylist(playlist.getId(), true, false, true).getPageReferences();
		assertEquals(1, pageReferences.size());
		assertEquals("p2", pageReferences.get(0).getPage().getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddPagesToNonExistingPlaylist() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlistService.createPlaylist(playlist);
		playlistService.addPage(-1, new Page("p1"));
	}

	@Test
	public void testGetPlaylistPages() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist = playlistService.createPlaylist(playlist);

		List<PageReference> pageReferences = getPlaylist(playlist.getId()).getPageReferences();
		assertEquals(2, pageReferences.size());
		assertEquals("p2", pageReferences.get(1).getPage().getName());
	}

	@Test
	public void testDeletePage() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist = playlistService.createPlaylist(playlist);

		assertEquals(2, getPlaylist(playlist.getId()).getPageReferences().size());
		playlistService.deletePage(playlist.getId(), playlist.getPageReferences().get(0).getId());

		assertEquals(1, getPlaylist(playlist.getId()).getPageReferences().size());
		assertEquals("p2", getPlaylist(playlist.getId()).getPageReferences().get(0).getPage().getName());
	}

	@Test
	public void testMovePage() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));
		Page page3 = pageService.createPage(new Page("p3"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist.addPageReference(new PageReference(page3));
		playlist = playlistService.createPlaylist(playlist);
		long id = playlist.getId();

		assertEquals("p1", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(1).getPage().getName());

		// swap
		playlistService.movePage(id, getPlaylist(id).getPageReferences().get(1).getId(), 0);
		assertEquals(3, getPlaylist(id).getPageReferences().size());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p1", getPlaylist(id).getPageReferences().get(1).getPage().getName());

		// switch back
		playlistService.movePage(id, getPlaylist(id).getPageReferences().get(0).getId(), 1);
		assertEquals(3, getPlaylist(id).getPageReferences().size());
		assertEquals("p1", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(1).getPage().getName());

		// switch to same position
		playlistService.movePage(id, getPlaylist(id).getPageReferences().get(0).getId(), 0);
		assertEquals(3, getPlaylist(id).getPageReferences().size());
		assertEquals("p1", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(1).getPage().getName());
		assertEquals("p3", getPlaylist(id).getPageReferences().get(2).getPage().getName());

		// add some more pages
		Page page4 = pageService.createPage(new Page("p4"));
		Page page5 = pageService.createPage(new Page("p5"));
		playlistService.addPage(id, page4);
		playlistService.addPage(id, page5);

		assertEquals(5, getPlaylist(id).getPageReferences().size());
		assertEquals("p1", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(1).getPage().getName());
		assertEquals("p3", getPlaylist(id).getPageReferences().get(2).getPage().getName());
		assertEquals("p4", getPlaylist(id).getPageReferences().get(3).getPage().getName());
		assertEquals("p5", getPlaylist(id).getPageReferences().get(4).getPage().getName());
		playlistService.movePage(id, getPlaylist(id).getPageReferences().get(3).getId(), 2);
		assertEquals(5, getPlaylist(id).getPageReferences().size());
		assertEquals("p1", getPlaylist(id).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(id).getPageReferences().get(1).getPage().getName());
		assertEquals("p4", getPlaylist(id).getPageReferences().get(2).getPage().getName());
		assertEquals("p3", getPlaylist(id).getPageReferences().get(3).getPage().getName());
		assertEquals("p5", getPlaylist(id).getPageReferences().get(4).getPage().getName());
	}

	@Test
	public void testMovePageToSameIdx() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist = playlistService.createPlaylist(playlist);
		assertEquals("p1", getPlaylist(playlist.getId()).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(playlist.getId()).getPageReferences().get(1).getPage().getName());

		playlistService.movePage(playlist.getId(), playlist.getPageReferences().get(1).getId(), 1);
		assertEquals(2, getPlaylist(playlist.getId()).getPageReferences().size());
		assertEquals("p1", getPlaylist(playlist.getId()).getPageReferences().get(0).getPage().getName());
		assertEquals("p2", getPlaylist(playlist.getId()).getPageReferences().get(1).getPage().getName());
	}

	@Test
	public void testSetCustomPageDuration() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist = playlistService.createPlaylist(playlist);

		PageReference pageUpdate = new PageReference();
		pageUpdate.setPageDisplayDurationOverride(99);
		playlistService.updatePageReference(getPlaylist(playlist.getId()).getPageReferences().get(0).getId(),
				pageUpdate);

		assertEquals(99, getPlaylist(playlist.getId()).getPageReferences().get(0).getPageDisplayDurationOverride());
		assertEquals(0, getPlaylist(playlist.getId()).getPageReferences().get(1).getPageDisplayDurationOverride());
	}

	@Test
	public void testCopyPlaylist() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));

		Playlist playlist = new Playlist("d1");
		playlist.addPageReference(new PageReference(page1));
		playlist.addPageReference(new PageReference(page2));
		playlist = playlistService.createPlaylist(playlist);

		PageReference pageUpdate = new PageReference();
		pageUpdate.setPageDisplayDurationOverride(99);
		playlistService.updatePageReference(getPlaylist(playlist.getId()).getPageReferences().get(0).getId(),
				pageUpdate);

		Playlist copy = playlistService.copyPlaylist(playlist.getId(), new Playlist("d2"));
		List<Playlist> playlists = getPlaylists(false);
		assertEquals(2, playlists.size());
		assertEquals("d2", playlists.get(1).getName());
		assertEquals(copy.getId(), playlists.get(1).getId());
		assertEquals(2, playlists.get(1).getPageReferences().size());
		assertEquals(99, playlists.get(1).getPageReferences().get(0).getPageDisplayDurationOverride());

	}

	@Test(expected = WebApplicationException.class)
	public void testCopyNonExistentPlaylist() throws Exception {
		playlistService.copyPlaylist(-1, new Playlist());
	}

	@Test
	public void testAddOwners() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		assertEquals(1, getPlaylist(playlist.getId()).getOwners().size());

		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(2, getPlaylist(playlist.getId()).getOwners().size());
		List<Owner> owners = getPlaylist(playlist.getId()).getOwners();
		assertEquals("i1", owners.get(1).getUser().getUserId());

		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i2")) });
		assertEquals(3, getPlaylist(playlist.getId()).getOwners().size());
	}

	@Test
	public void testAddMultipleOwners() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		assertEquals(1, getPlaylist(playlist.getId()).getOwners().size());

		playlistService.addOwners(playlist.getId(),
				new Owner[] { new Owner(new User("i1")), new Owner(new User("i2")) });
		assertEquals(3, getPlaylist(playlist.getId()).getOwners().size());
		List<Owner> owners = getPlaylist(playlist.getId()).getOwners();
		assertEquals("i1", findOwner("i1", owners).getUser().getUserId());
		assertEquals("i2", findOwner("i2", owners).getUser().getUserId());
	}

	@Test
	public void testNoOwner() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		assertEquals(1, getPlaylists(false).size());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		playlistService.request = requestMock;
		assertEquals(0, getPlaylists(false).size());

		// switch user to add owner
		requestMock = new HttpServletRequestMock(simpleUser);
		playlistService.request = requestMock;
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(simpleUser2) });

		// switch back
		requestMock = new HttpServletRequestMock(simpleUser2);
		playlistService.request = requestMock;
		assertEquals(1, getPlaylists(false).size());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddDuplicateOwner() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlistService.createPlaylist(playlist);
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1")) });
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("wrongUserId")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner2() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1i2")) });
	}

	@Test
	public void testDeleteOwner() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlist = playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i1")) });
		playlist = playlistService.addOwners(playlist.getId(), new Owner[] { new Owner(new User("i2")) });

		assertEquals(3, getPlaylist(playlist.getId()).getOwners().size());
		playlistService.deleteOwner(playlist.getId(), playlist.getOwners().get(1).getId());

		assertEquals(2, getPlaylist(playlist.getId()).getOwners().size());
		assertEquals("i2", getPlaylist(playlist.getId()).getOwners().get(1).getUser().getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testDeleteLastOwner() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);
		playlistService.deleteOwner(playlist.getId(), playlist.getOwners().get(0).getId());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdatePlaylistByNonOwner() throws Exception {
		Playlist playlist = playlistService.createPlaylist(new Playlist("d1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		playlistService.request = requestMock;

		playlistService.updatePlaylist(playlist.getId(), new Playlist("d2"));
	}

	@Test
	public void testAddPlaylist() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));
		Page page3 = pageService.createPage(new Page("p3"));

		Playlist playlist1 = new Playlist("d1");
		playlist1.addPageReference(new PageReference(page1));
		playlist1 = playlistService.createPlaylist(playlist1);
		assertEquals(1, playlistService.getPlaylist(playlist1.getId(), false, false, true).getPageReferences().size());

		Playlist playlist2 = new Playlist("d2");
		playlist2.addPageReference(new PageReference(page2));
		playlist2.addPageReference(new PageReference(page3));
		playlist2 = playlistService.createPlaylist(playlist2);
		assertEquals(2, playlistService.getPlaylist(playlist2.getId(), false, false, true).getPageReferences().size());

		playlist1 = playlistService.addPlaylist(playlist1.getId(), playlist2);

		List<PageReference> pageReferences = getPlaylist(playlist1.getId()).getPageReferences();
		assertEquals(2, pageReferences.size());
		assertEquals(PageRefType.PAGE, pageReferences.get(0).getRefType());
		assertEquals(PageRefType.PLAYLIST, pageReferences.get(1).getRefType());

		// expand must return all pages
		pageReferences = playlistService.getPlaylist(playlist1.getId(), false, true, true).getPageReferences();
		assertEquals(3, pageReferences.size());

		playlist1 = playlistService.addPlaylist(playlist1.getId(), playlist2);
		pageReferences = playlistService.getPlaylist(playlist1.getId(), false, true, true).getPageReferences();
		assertEquals(5, pageReferences.size());

		// recursion must be detected
		Playlist playlist3 = new Playlist("d3");
		playlist3.addPageReference(new PageReference(playlist1));
		playlist3 = playlistService.createPlaylist(playlist3);

		playlist1 = playlistService.addPlaylist(playlist1.getId(), playlist3);
		pageReferences = playlistService.getPlaylist(playlist1.getId(), false, true, true).getPageReferences();
		assertEquals(5, pageReferences.size());
		assertEquals("p1", pageReferences.get(0).getPage().getName());
		assertEquals("p2", pageReferences.get(1).getPage().getName());
		assertEquals("p3", pageReferences.get(2).getPage().getName());
		assertEquals("p2", pageReferences.get(3).getPage().getName());
		assertEquals("p3", pageReferences.get(4).getPage().getName());

		// make sure playlist overview can expand
		List<Playlist> playlists = getPlaylists(false);
		assertEquals(4, playlists.get(0).getPageReferences().size());
		assertEquals(2, playlists.get(1).getPageReferences().size());
		assertEquals(1, playlists.get(2).getPageReferences().size());

		playlists = getPlaylists(true);
		assertEquals(5, playlists.get(0).getPageReferences().size());
		assertEquals(2, playlists.get(1).getPageReferences().size());
		assertEquals(5, playlists.get(2).getPageReferences().size());

		// make sure screen API always expands
		Screen screen = new Screen("s1", playlist1);
		screen.setKey("sk1");
		screen = screenService.createScreen(screen);
		assertEquals(5, screen.getPlaylist().getPageReferences().size());
		assertEquals(5, screenService.getScreen(screen.getId(), false, false).getPlaylist().getPageReferences().size());
		assertEquals(5, screenService.getScreenByKey("sk1", false, false, false, null).getPlaylist().getPageReferences()
				.size());
		assertEquals(5, screenService.getScreens(null).get(0).getPlaylist().getPageReferences().size());

		// make sure update afterward expanding is not broken
		screen = screenService.addOwners(screen.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(5, screenService.getScreen(screen.getId(), false, false).getPlaylist().getPageReferences().size());
	}

	@Test
	public void testRepeatingTiles() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));
		Page page3 = pageService.createPage(new Page("p3"));
		Page page4 = pageService.createPage(new Page("p4"));
		Page page5 = pageService.createPage(new Page("p5"));

		Playlist playlist1 = new Playlist("d1");
		PageReference ref1 = new PageReference(page1);
		PageReference ref2 = new PageReference(page2);
		PageReference ref3 = new PageReference(page3);
		PageReference ref4 = new PageReference(page4);
		PageReference ref5 = new PageReference(page5);

		ref1.setRepeatEveryIteration(2);
		ref2.setRepeatEveryPage(2);
		ref3.setRepeatEveryIteration(3);
		ref4.setRepeatEveryPage(3);

		playlist1.addPageReference(ref1);
		playlist1.addPageReference(ref2);
		playlist1.addPageReference(ref3);
		playlist1.addPageReference(ref4);
		playlist1.addPageReference(ref5);
		playlist1 = playlistService.createPlaylist(playlist1);

		List<PageReference> pageReferences = getPlaylist(playlist1.getId()).getPageReferences();
		assertEquals(5, pageReferences.size());

		// expand must return repetitions all pages
		pageReferences = playlistService.getPlaylist(playlist1.getId(), false, true, true).getPageReferences();

		int seq[] = { 1, 2, 3, 2, 4, 2, 5, 4, 2, 5, 4, 2, 1, 4, 2, 5, 4, 2, 3, 4, 2, 5, 4, 2, 1, 4, 2, 5, 4, 2, 5 };
		for (int i = 0; i < seq.length; i++) {
			assertEquals("Error at position " + i, "p" + seq[i], pageReferences.get(i).getPage().getName());
		}
		assertEquals(31, pageReferences.size());
	}

	@Test
	public void testBreakInfiniteRecursion() throws Exception {
		Playlist playlist1 = new Playlist("d1");
		Playlist playlist2 = new Playlist("d2");
		Playlist playlist3 = new Playlist("d3");
		playlist1.setListPublicly(true);
		playlist2.setListPublicly(true);
		playlist3.setListPublicly(true);
		playlist1 = playlistService.createPlaylist(playlist1);
		playlist2 = playlistService.createPlaylist(playlist2);
		playlist3 = playlistService.createPlaylist(playlist3);

		playlist1 = playlistService.addPlaylist(playlist1.getId(), playlist2);
		playlist2 = playlistService.addPlaylist(playlist2.getId(), playlist3);
		playlist3 = playlistService.addPlaylist(playlist3.getId(), playlist1);

		List<Playlist> playlists = playlistService.getPlaylistCatalog(0);
		assertEquals(3, playlists.size());
		// FIXME: not working yet
		// assertNull(playlists.get(0).getPageReferences().get(0).getPlaylist().getPageReferences().get(0).getPlaylist());
	}

	@Test
	public void testGetPublicPlaylists() throws Exception {
		assertEquals(0, getPlaylists(false).size());

		Playlist playlist1 = new Playlist("d1");
		Playlist playlist2 = new Playlist("d2");
		Playlist playlist3 = new Playlist("d3");
		playlist2.setListPublicly(true);
		playlist3.setListPublicly(true);
		playlist1 = playlistService.createPlaylist(playlist1);
		playlist2 = playlistService.createPlaylist(playlist2);
		playlist3 = playlistService.createPlaylist(playlist3);

		assertEquals(3, playlistService.getPlaylistCatalog(0).size());
		assertEquals("d1", playlistService.getPlaylistCatalog(0).get(0).getName());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		playlistService.request = requestMock;

		assertEquals(2, playlistService.getPlaylistCatalog(0).size());
		assertEquals("d2", playlistService.getPlaylistCatalog(0).get(0).getName());

		assertEquals(1, playlistService.getPlaylistCatalog(playlist2.getId()).size());
		assertEquals("d3", playlistService.getPlaylistCatalog(playlist2.getId()).get(0).getName());
	}

	@Test
	public void testRemovePlaylistMetrics() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		Playlist playlist1 = new Playlist("d1");
		playlist1 = playlistService.createPlaylist(playlist1);
		playlist1 = playlistService.addPage(playlist1.getId(), page1);

		// make sure metrics are stripped
		Screen screen = new Screen("s1", playlist1);
		screen = screenService.createScreen(screen);

		Screen screenData = new Screen();
		screenData.setMetric_browser("browser");
		screenData.setMetric_currentPageId(playlist1.getPageReferences().get(0).getId());
		screenData.setPlaylist(playlist1);
		screenService.updateScreenStatstics(screen.getId(), screenData);

		playlist1 = playlistService.getPlaylist(playlist1.getId(), false, false, true);
		assertEquals(1, playlist1.getPageReferences().get(0).getPage().getStats_viewCount());

		playlist1 = playlistService.getPlaylist(playlist1.getId(), false, false, false);
		assertEquals(0, playlist1.getPageReferences().get(0).getPage().getStats_viewCount());
	}

	@Test
	public void testAddFiles() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);

		File file1 = fileService.createFile("f1", "jpg", "f1", 99);
		File file2 = fileService.createFile("f2", "mp4", "f2", 99);
		playlistService.addFile(playlist.getId(), file1);
		playlistService.addFile(playlist.getId(), file2);

		List<PageReference> pageReferences = playlistService.getPlaylist(playlist.getId(), false, false, true)
				.getPageReferences();
		assertEquals(PageReference.PageRefType.FILE, pageReferences.get(0).getRefType());
		assertEquals("f1", pageReferences.get(0).getFile().getName());
		assertEquals("f2", pageReferences.get(1).getFile().getName());

		// expand should materialize virtual pages on the fly
		pageReferences = playlistService.getPlaylist(playlist.getId(), false, true, true).getPageReferences();
		assertEquals(PageReference.PageRefType.PAGE, pageReferences.get(0).getRefType());
		assertEquals("f1", pageReferences.get(0).getPage().getName());
		assertEquals("f2", pageReferences.get(1).getPage().getName());
		assertEquals(PageType.IMAGE, pageReferences.get(0).getPage().getPageType());
		assertEquals(PageType.MOVIE, pageReferences.get(1).getPage().getPageType());
		assertEquals(file1.getOwners().size(), pageReferences.get(0).getPage().getOwners().size());
		assertTrue(file1.getOwners().get(0).isContact());
	}

	@Test
	public void testGetReferences() throws Exception {
		Playlist playlist1 = new Playlist("d1");
		playlist1 = playlistService.createPlaylist(playlist1);

		Playlist playlist2 = new Playlist("d2");
		playlist2 = playlistService.createPlaylist(playlist2);

		Playlist playlist3 = new Playlist("d3");
		playlist3 = playlistService.createPlaylist(playlist3);
		playlist3 = playlistService.addPlaylist(playlist3.getId(), playlist2);

		Playlist playlist4 = new Playlist("d4");
		playlist4 = playlistService.createPlaylist(playlist4);
		playlist4 = playlistService.addPlaylist(playlist4.getId(), playlist2);

		// an unreferenced playlist
		Playlist playlist5 = new Playlist("d5");
		playlist5 = playlistService.createPlaylist(playlist5);

		screenService.createScreen(new Screen("s1", playlist1));
		screenService.createScreen(new Screen("s2", playlist2));
		screenService.createScreen(new Screen("s3", playlist2));
		Screen screen4 = screenService.createScreen(new Screen("s4", playlist1));
		screenService.createScreen(new Screen("s5"));

		// one screen where user is not owner
		screen4.getOwners().clear();
		screen4 = ScreenDAO.save(screen4);

		// one playlist where user is not owner
		playlist4.getOwners().clear();
		playlist4 = PlaylistDAO.save(playlist4);

		PlaylistReferences refs = playlistService.getReferences(playlist1.getId());
		assertEquals(1, refs.getUserScreens().size());
		assertEquals(1, refs.getForeignScreens());
		assertEquals(0, refs.getUserPlaylists().size());
		assertEquals(0, refs.getForeignPlaylists());

		refs = playlistService.getReferences(playlist2.getId());
		assertEquals(2, refs.getUserScreens().size());
		assertEquals(0, refs.getForeignScreens());
		assertEquals(1, refs.getUserPlaylists().size());
		assertEquals(1, refs.getForeignPlaylists());

		refs = playlistService.getReferences(playlist5.getId());
		assertEquals(0, refs.getUserScreens().size());
		assertEquals(0, refs.getForeignScreens());
		assertEquals(0, refs.getUserPlaylists().size());
		assertEquals(0, refs.getForeignPlaylists());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesForNonExistingPlaylist() throws Exception {
		playlistService.getReferences(-1);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesAsUnauthorized() throws Exception {
		Playlist playlist = playlistService.createPlaylist(new Playlist("d1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.getReferences(playlist.getId());
	}

	private List<Playlist> getPlaylists(boolean expand) {
		return playlistService.getPlaylists(expand, null);
	}

	private Playlist getPlaylist(long id) {
		return playlistService.getPlaylist(id, false, false, true);
	}

}
