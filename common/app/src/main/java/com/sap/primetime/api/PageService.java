package com.sap.primetime.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PageReferenceDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.dto.PageReferences;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.util.Consts;

import io.swagger.annotations.Api;

@Api(value = "Pages")
@Path("pageservice")
public class PageService extends BasicService implements OwnerAPISupport<Page> {
	private Map<Long, Page> drafts = new HashMap<>();

	@GET
	@Path("/pages")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Page> getPages(@QueryParam("userId") String userId) {
		return PageDAO.getPagesForUser(getForeignUser(userId));
	}

	@GET
	@Path("/catalog")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Page> getPageCatalog() {
		return PageDAO.getPageCatalog(userUtil.getLoggedInDBUser(request));
	}

	@GET
	@Path("/advertisedpages")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Page> getAdvertisedPages(@DefaultValue("0") @QueryParam("maxResults") int maxResults) {
		return PageDAO.getAdvertisedPages(maxResults);
	}

	@GET
	@Path("/pages/{pageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page getPage(@PathParam("pageId") long pageId) {
		return getExistingPage(pageId, false);
	}

	@PUT
	@Path("/pages/{pageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page updatePage(@PathParam("pageId") long pageId, Page pageData) {
		Page existingPage = getExistingPage(pageId, true);

		sanitizePageData(pageData);
		if (StringUtils.isEmpty(pageData.getName())) {
			throwBadRequest();
		}

		File file = existingPage.getAutoScreenshot();

		existingPage.setDescription(pageData.getDescription());
		existingPage.setFile(pageData.getFile());
		existingPage.setGridX(pageData.getGridX());
		existingPage.setGridY(pageData.getGridY());
		existingPage.setGridRatio(pageData.getGridRatio());
		existingPage.setListPublicly(pageData.isListPublicly());
		existingPage.setMediasharePrivate(pageData.isMediasharePrivate());
		existingPage.setName(pageData.getName());
		existingPage.setPage(pageData.getPage());
		existingPage.setPageType(pageData.getPageType());
		existingPage.setReloadInterval(pageData.getReloadInterval());
		existingPage.setText(pageData.getText());
		existingPage.setTitle(pageData.getTitle());
		existingPage.setUrl(pageData.getUrl());
		existingPage.setAutoScreenshot(null);

		existingPage = PageDAO.save(existingPage);
		if (file != null) {
			new FileDAO().deleteById(file.getId());
		}

		return existingPage;
	}

	@PUT
	@Path("/pages/{pageId}/templatevalues")
	@Produces(MediaType.APPLICATION_JSON)
	public Page updateTemplateValues(@PathParam("pageId") long pageId, Page pageData) {
		Page existingPage = getExistingPage(pageId, false);
		if (!isAdmin() && !inAdminOwners(existingPage.getOwners()) && !inContentOwners(existingPage.getOwners())) {
			throwUnauthorized();
		}

		sanitizePageData(pageData);
		existingPage.setTemplateValues(pageData.getTemplateValues());

		return PageDAO.save(existingPage);
	}

	@POST
	@Path("/pages")
	@Consumes(MediaType.APPLICATION_JSON)
	public Page createPage(Page page) {
		sanitizePageData(page);
		if (StringUtils.isEmpty(page.getName())) {
			throwBadRequest();
		}

		User user = userUtil.getLoggedInDBUser(request);
		Owner owner = new Owner(user);
		owner.setContact(true);
		page.addOwner(owner);

		return PageDAO.saveNew(page);
	}

	@POST
	@Path("/pages/{pageId}/screenshot")
	public Response uploadScreenshot(@PathParam("pageId") long pageId) {
		Page existingPage = getExistingPage(pageId, true);
		File existingFile = doUploadScreenshot(String.valueOf(pageId), "int_pagescreenhot_", Consts.xDim, Consts.yDim);

		existingPage.setScreenshot(existingFile);
		PageDAO.save(existingPage);

		return RESPONSE_OK;
	}

	@DELETE
	@Path("/pages/{pageId}/screenshot")
	public Response deleteScreenshot(@PathParam("pageId") long pageId) {
		Page existingPage = getExistingPage(pageId, true);

		if (existingPage.getScreenshot() == null) {
			throwNotFound("Page does not contain any custom screenshot.");
		}
		File screenshot = existingPage.getScreenshot();

		existingPage.setScreenshot(null);
		PageDAO.save(existingPage);

		getECM().deleteDocumentByName(screenshot.getFileKey(), false);
		new FileDAO().deleteById(screenshot.getId());

		return RESPONSE_OK;
	}

	@GET
	@Path("/drafts/{pageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page getDraft(@PathParam("pageId") long pageId) {
		return drafts.containsKey(pageId) ? drafts.get(pageId) : getPage(pageId);
	}

	@PUT
	@Path("/drafts/{pageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Page setDraft(@PathParam("pageId") long pageId, Page page) {
		sanitizePageData(page);

		drafts.put(pageId, page);

		return page;
	}

	@DELETE
	@Path("/drafts/{pageId}")
	public Response deleteDraft(@PathParam("pageId") long pageId) {
		drafts.remove(pageId);

		return RESPONSE_OK;
	}

	private void sanitizePageData(Page page) {
		page.setName(sanitize(page.getName()));
		page.setTitle(sanitize(page.getTitle()));
		page.setDescription(sanitize(page.getDescription()));
		page.setUrl(sanitize(page.getUrl()));

		if (page.getFile() != null && new FileDAO().getById(page.getFile().getId()) == null) {
			page.setFile(null);
		}
		if (page.getText() != null && page.getPageType() == PageType.HTML) {
			page.setText(Jsoup.clean(page.getText(), Whitelist.relaxed().addAttributes(":all", "style")));
		}

		// nullify empty strings
		if (page.getTitle() != null && page.getTitle().length() == 0) {
			page.setTitle(null);
		}
		if (page.getDescription() != null && page.getDescription().length() == 0) {
			page.setDescription(null);
		}
		if (page.getUrl() != null && page.getUrl().length() == 0) {
			page.setUrl(null);
		}
		if (page.getText() != null && page.getText().length() == 0) {
			page.setText(null);
		}
	}

	@DELETE
	@Path("/pages/{pageId}")
	public Response deletePage(@PathParam("pageId") long pageId) {
		Page existingPage = getExistingPage(pageId, true);

		// remove references in existing playlists
		new PageReferenceDAO().deleteByPage(existingPage);
		for (Owner owner : existingPage.getOwners()) {
			new OwnerDAO().deleteById(owner.getId());
		}

		// TODO: delete screenshots

		return new PageDAO().deleteById(pageId) ? RESPONSE_OK : RESPONSE_BAD;
	}

	@POST
	@Path("/pages/{pageId}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	public Page copyPage(@PathParam("pageId") long pageId, Page newPage) {
		Page existingPage = getExistingPage(pageId, false);

		Page copiedPage = new Page(existingPage);
		copiedPage.setName(newPage.getName());
		copiedPage = createPage(copiedPage);

		return copiedPage;
	}

	@GET
	@Path("/pages/{pageId}/references")
	@Produces(MediaType.APPLICATION_JSON)
	public PageReferences getReferences(@PathParam("pageId") long pageId) {
		Page existingPage = getExistingPage(pageId, false);
		if (!existingPage.isListPublicly() && !this.isAdmin() && !inAdminOwners(existingPage.getOwners())) {
			throwUnauthorized();
		}

		PageReferences refs = new PageReferences();

		List<Playlist> playlists = PlaylistDAO.getPlaylistsByPage(existingPage);
		for (Playlist playlist : playlists) {
			if (this.isAdmin() || inAdminOwners(playlist.getOwners())) {
				refs.addUserPlaylist(playlist);
			} else {
				refs.setForeignPlaylists(refs.getForeignPlaylists() + 1);
			}
		}

		return refs;
	}

	@Override
	@POST
	@Path("/pages/{pageId}/owners")
	@Consumes(MediaType.APPLICATION_JSON)
	public Page addOwners(@PathParam("pageId") long pageId, Owner[] ownerData) {
		Page existingPage = getExistingPage(pageId, true);
		doAddOwners(existingPage, ownerData);

		return PageDAO.save(existingPage);
	}

	@Override
	@PUT
	@Path("/pages/{pageId}/owners/{ownerId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Page updateOwner(@PathParam("pageId") long pageId, @PathParam("ownerId") long ownerId, Owner owner) {
		Page existingPage = getExistingPage(pageId, true);

		boolean found = false;
		for (Owner pageOwner : existingPage.getOwners()) {
			if (pageOwner.getId() == ownerId) {
				pageOwner.setContact(owner.isContact());
				found = true;
				break;
			}
		}
		if (!found) {
			throwNotFound("Owner does not exist");
		}

		return PageDAO.save(existingPage);
	}

	@Override
	@DELETE
	@Path("/pages/{pageId}/owners/{ownerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page deleteOwner(@PathParam("pageId") long pageId, @PathParam("ownerId") long ownerId) {
		Page existingPage = getExistingPage(pageId, true);
		doDeleteOwner(existingPage, ownerId);

		return PageDAO.save(existingPage);
	}

}
