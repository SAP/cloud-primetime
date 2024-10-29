package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.dto.PageReferences;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Owner.Role;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.HttpServletRequestMock;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.testutil.UploadHelper;

public class PageServiceTest extends PrimeTimeTest {
	private PageService pageService;
	private PlaylistService playlistService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		pageService = new PageService();
		pageService.request = requestMock;

		playlistService = new PlaylistService();
		playlistService.request = requestMock;
	}

	@Test
	public void testCreatePage() throws Exception {
		assertEquals(0, getPages().size());

		pageService.createPage(new Page("p1"));
		assertEquals(1, getPages().size());

		pageService.createPage(new Page("p2"));
		List<Page> pages = getPages();
		assertEquals(2, pages.size());
		assertEquals(1, pages.get(0).getOwners().size());
		assertEquals(simpleUser.getUserId(), pages.get(0).getOwners().get(0).getUser().getUserId());
	}

	@Test
	public void testGetPagesForUser() throws Exception {
		pageService.createPage(new Page("p1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.createPage(new Page("p2"));
		List<Page> pages = pageService.getPages(simpleUser2.getUserId());
		assertEquals(1, pages.size());
		assertEquals("p2", pages.get(0).getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetPagesForOtherUser() throws Exception {
		pageService.createPage(new Page("p1"));
		pageService.getPages(simpleUser2.getUserId());
	}

	@Test
	public void testCopyPage() throws Exception {
		Page page = pageService.createPage(new Page("p1"));
		page = pageService.copyPage(page.getId(), new Page("p2"));

		List<Page> pages = getPages();
		assertEquals(2, pages.size());
		assertEquals("p2", pages.get(1).getName());
		assertEquals(page.getId(), pages.get(1).getId());
	}

	@Test(expected = WebApplicationException.class)
	public void testCopyNonExistentPage() throws Exception {
		pageService.copyPage(-1, new Page());
	}

	@Test
	public void testPageScreenshot() throws Exception {
		Page page = pageService.createPage(new Page("p1"));
		assertNull(getPages().get(0).getScreenshot());

		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		pageService.uploadScreenshot(page.getId());
		assertNotNull(getPages().get(0).getScreenshot());
	}

	@Test
	public void testGetContentManagerPages() throws Exception {
		Page page1 = pageService.createPage(new Page("p1"));
		pageService.addOwners(page1.getId(), new Owner[] { new Owner(simpleUser2, Role.CONTENT_MANAGER) });
		pageService.createPage(new Page("p2"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		List<Page> pages = getPages();
		assertEquals(1, pages.size());
		assertEquals(2, pages.get(0).getOwners().size());
	}

	@Test
	public void testGetCatalog() throws Exception {
		assertEquals(0, getPages().size());

		Page page1 = new Page("p1");
		Page page2 = new Page("p2");
		page2.setListPublicly(true);

		pageService.createPage(page1);
		pageService.createPage(page2);
		assertEquals(2, pageService.getPageCatalog().size());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		assertEquals(1, pageService.getPageCatalog().size());
		assertEquals("p2", pageService.getPageCatalog().get(0).getName());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingPage() throws Exception {
		pageService.getPage(-1);
	}

	@Test
	public void testGetPage() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		assertNotNull(pageService.getPage(page.getId()));
		assertEquals("p1", pageService.getPage(page.getId()).getName());
	}

	@Test
	public void testUpdatePage() throws Exception {
		Page page = pageService.createPage(new Page("p1"));
		pageService.updatePage(page.getId(), new Page("p2"));
		assertEquals("p2", pageService.getPage(page.getId()).getName());
	}

	@Test
	public void testUpdateLongUrl() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		String longUrl = StringUtils.repeat("url", 1000);
		page.setUrl(longUrl);

		pageService.updatePage(page.getId(), page);
		assertEquals(longUrl, pageService.getPage(page.getId()).getUrl());
	}

	@Test
	public void testUpdateTemplateValues() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		Page pageData = new Page("p2");
		String values = "{\"key\": \"value\"}";

		pageData.setTemplateValues(values);
		pageService.updateTemplateValues(page.getId(), pageData);

		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals(values, pageService.getPage(page.getId()).getTemplateValues());
	}

	@Test
	public void testUpdateTemplateValuesWithContentOnlyPermissions() throws Exception {
		Page page = pageService.createPage(new Page("p1"));
		pageService.addOwners(page.getId(), new Owner[] { new Owner(simpleUser2, Role.CONTENT_MANAGER) });

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		Page pageData = new Page("p2");
		String values = "{\"key\": \"value\"}";

		pageData.setTemplateValues(values);
		pageService.updateTemplateValues(page.getId(), pageData);

		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals(values, pageService.getPage(page.getId()).getTemplateValues());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdatePageWithContentOwnerRole() throws Exception {
		Page page = pageService.createPage(new Page("p1"));
		pageService.addOwners(page.getId(), new Owner[] { new Owner(simpleUser2, Role.CONTENT_MANAGER) });

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.updatePage(page.getId(), new Page("p2"));
	}

	@Test
	public void testInsecureHTML() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		String html = "<strong>text</strong><script>alert(123);</script>";
		page.setPageType(PageType.HTML);
		page.setText(html);

		pageService.updatePage(page.getId(), page);
		assertEquals("<strong>text</strong>", pageService.getPage(page.getId()).getText());
	}

	@Test
	public void testDeletePage() throws Exception {
		pageService.createPage(new Page("p1"));
		assertEquals(1, getPages().size());
		pageService.deletePage(getPages().get(0).getId());
		assertEquals(0, getPages().size());
	}

	@Test
	public void testDeletePageUsedInPlaylist() throws Exception {
		Playlist playlist = new Playlist("d1");
		playlist = playlistService.createPlaylist(playlist);

		Page page1 = pageService.createPage(new Page("p1"));
		playlistService.addPage(playlist.getId(), page1);
		assertEquals(1, playlistService.getPlaylist(playlist.getId(), false, false, true).getPageReferences().size());

		pageService.deletePage(page1.getId());
		assertEquals(0, getPages().size());
		assertEquals(0, playlistService.getPlaylist(playlist.getId(), false, false, true).getPageReferences().size());
	}

	@Test
	public void testCreateDraft() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals("p1", pageService.getDraft(page.getId()).getName());

		page.setName("p2");
		pageService.setDraft(page.getId(), page);

		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals("p2", pageService.getDraft(page.getId()).getName());

		page.setName("p3");
		pageService.setDraft(page.getId(), page);
		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals("p3", pageService.getDraft(page.getId()).getName());

		pageService.deleteDraft(page.getId());
		assertEquals("p1", pageService.getPage(page.getId()).getName());
		assertEquals("p1", pageService.getDraft(page.getId()).getName());
	}

	@Test
	public void testAddOwners() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		assertEquals(1, pageService.getPage(page.getId()).getOwners().size());

		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(2, pageService.getPage(page.getId()).getOwners().size());
		List<Owner> owners = pageService.getPage(page.getId()).getOwners();
		assertEquals("i1", owners.get(1).getUser().getUserId());

		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i2")) });
		assertEquals(3, pageService.getPage(page.getId()).getOwners().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("wrongUserId")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner2() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1i2")) });
	}

	@Test
	public void testNoOwner() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		assertEquals(1, getPages().size());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;
		assertEquals(0, getPages().size());

		// switch user to add owner
		requestMock = new HttpServletRequestMock(simpleUser);
		pageService.request = requestMock;
		pageService.addOwners(page.getId(), new Owner[] { new Owner(simpleUser2) });

		// switch back
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;
		assertEquals(1, getPages().size());
	}

	@Test(expected = WebApplicationException.class)
	@Ignore
	public void testAddDuplicateOwner() throws Exception {
		Page page = new Page("p1");
		pageService.createPage(page);
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1")) });
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1")) });
	}

	@Test
	public void testDeleteOwner() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1")) });
		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i2")) });
		page = pageService.getPage(page.getId());

		assertEquals(3, pageService.getPage(page.getId()).getOwners().size());
		pageService.deleteOwner(page.getId(), page.getOwners().get(1).getId());

		assertEquals(2, pageService.getPage(page.getId()).getOwners().size());
		assertEquals("i2", pageService.getPage(page.getId()).getOwners().get(1).getUser().getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testUpdatePageByNonOwner() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.updatePage(page.getId(), new Page("p2"));
	}

	@Test
	public void testPublicOwners() throws Exception {
		Page page = new Page("p1");
		page = pageService.createPage(page);
		simpleUser.updateDynamicValues();
		assertEquals(simpleUser.getDisplayName(), pageService.getPage(page.getId()).getOwnersDisplayText());

		pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(simpleUser.getDisplayName() + ", i1", pageService.getPage(page.getId()).getOwnersDisplayText());

		page = pageService.addOwners(page.getId(), new Owner[] { new Owner(new User("i2")) });
		Owner owner = page.getOwners().get(2);
		owner.setContact(false);
		page = pageService.updateOwner(page.getId(), owner.getId(), owner);
		assertEquals(3, pageService.getPage(page.getId()).getOwners().size());
		assertEquals(simpleUser.getDisplayName() + ", i1", pageService.getPage(page.getId()).getOwnersDisplayText());
	}

	@Test
	public void testGetReferences() throws Exception {
		Playlist playlist1 = new Playlist("d1");
		playlist1 = playlistService.createPlaylist(playlist1);

		Playlist playlist2 = new Playlist("d2");
		playlist2 = playlistService.createPlaylist(playlist2);

		Playlist playlist3 = new Playlist("d3");
		playlist3 = playlistService.createPlaylist(playlist3);

		Page page1 = pageService.createPage(new Page("p1"));
		Page page2 = pageService.createPage(new Page("p2"));
		playlist1 = playlistService.addPage(playlist1.getId(), page1);
		playlist1 = playlistService.addPage(playlist1.getId(), page2);
		playlist2 = playlistService.addPage(playlist2.getId(), page1);
		playlist3 = playlistService.addPage(playlist3.getId(), page1);

		// one playlist where user is not owner
		playlist3.getOwners().clear();
		playlist3 = PlaylistDAO.save(playlist3);

		PageReferences refs = pageService.getReferences(page1.getId());
		assertEquals(2, refs.getUserPlaylists().size());
		assertEquals(1, refs.getForeignPlaylists());

		refs = pageService.getReferences(page2.getId());
		assertEquals(1, refs.getUserPlaylists().size());
		assertEquals(0, refs.getForeignPlaylists());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesForNonExistingPage() throws Exception {
		pageService.getReferences(-1);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesAsUnauthorized() throws Exception {
		Page page = pageService.createPage(new Page("p1"));

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.getReferences(page.getId());
	}

	@Test
	public void testGetPagesWithoutScreenshot() throws Exception {
		Page page1 = new Page("p1");
		page1.setPageType(PageType.TEXT);
		page1 = pageService.createPage(page1);

		Page page2 = new Page("p2");
		page2.setPageType(PageType.PDF);
		page2 = pageService.createPage(page2);

		Page page3 = new Page("p3");
		page3.setPageType(PageType.URL);
		page3 = pageService.createPage(page3);

		List<Page> pages = PageDAO.getNextPagesWithoutAutoScreenshot(10);
		assertEquals(2, pages.size());

		// upload a screenshot for one page
		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		pageService.uploadScreenshot(page1.getId());

		pages = PageDAO.getNextPagesWithoutAutoScreenshot(10);
		assertEquals(2, pages.size());
	}

	private List<Page> getPages() {
		return pageService.getPages(null);
	}
}
