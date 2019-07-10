package com.sap.primetime.api;

import java.util.ArrayList;
import java.util.List;

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

import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.PageReferenceDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.PlaylistReferences;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;

import io.swagger.annotations.Api;

@Api(value = "Playlists")
@Path("playlistservice")
public class PlaylistService extends BasicService implements OwnerAPISupport<Playlist> {
	@GET
	@Path("/playlists")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Playlist> getPlaylists(@DefaultValue("false") @QueryParam("expand") boolean expand,
			@QueryParam("userId") String userId) {
		List<Playlist> playlists = PlaylistDAO.getPlaylistsForUser(getForeignUser(userId));
		List<Playlist> result = new ArrayList<>();

		for (Playlist playlist : playlists) {
			if (expand) {
				playlist.expand();
			} else {
				// FIXME: ugly side-effects since setting one playlist null can cause the
				// result of another playlist to change, possibly detach entities first or
				// create new objects using copy-constructor
				playlist.breakRecursion();
			}

			result.add(playlist);
		}

		return result;
	}

	@GET
	@Path("/catalog")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Playlist> getPlaylistCatalog(@DefaultValue("0") @QueryParam("exclude") long excludeId) {
		List<Playlist> result = PlaylistDAO.getPlaylistCatalog(userUtil.getLoggedInDBUser(request), excludeId);

		for (Playlist playlist : result) {
			playlist.expand();
		}

		return result;
	}

	@GET
	@Path("/playlists/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist getPlaylist(@PathParam("playlistId") long playlistId,
			@DefaultValue("false") @QueryParam("visibleOnly") boolean visibleOnly,
			@DefaultValue("false") @QueryParam("expand") boolean expand,
			@DefaultValue("true") @QueryParam("includeMetrics") boolean includeMetrics) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, false);

		if (!existingPlaylist.isListPublicly() && !this.isAdmin() && !inAdminOwners(existingPlaylist.getOwners())) {
			throwUnauthorized();
		}

		if (visibleOnly) {
			for (PageReference pageReference : existingPlaylist.getPageReferences()) {
				if (!pageReference.isVisible()) {
					existingPlaylist.getPageReferences().remove(pageReference);
				}
			}
		}

		if (expand) {
			existingPlaylist.expand();
		} else {
			existingPlaylist.breakRecursion();
		}
		if (!includeMetrics) {
			removePlaylistMetrics(existingPlaylist);
		}
		return existingPlaylist;
	}

	@PUT
	@Path("/playlists/{playlistId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist updatePlaylist(@PathParam("playlistId") long playlistId, Playlist playlistData) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);

		existingPlaylist.setName(playlistData.getName());
		existingPlaylist.setPageDisplayDuration(playlistData.getPageDisplayDuration());
		existingPlaylist.setListPublicly(playlistData.isListPublicly());

		return PlaylistDAO.save(existingPlaylist);
	}

	@POST
	@Path("/playlists")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist createPlaylist(Playlist playlist) {
		playlist.setName(sanitize(playlist.getName()));

		if (StringUtils.isBlank(playlist.getName())) {
			throwBadRequest("Name must be supplied.");
		}

		User user = userUtil.getLoggedInDBUser(request);
		Owner owner = new Owner(user);
		owner.setContact(true);
		playlist.addOwner(owner);

		return PlaylistDAO.saveNew(playlist);
	}

	@POST
	@Path("/playlists/{playlistId}/pages")
	@Consumes(MediaType.APPLICATION_JSON)
	public Playlist addPage(@PathParam("playlistId") long playlistId, Page page) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		Page existingPage = getExistingPage(page.getId(), false);

		PageReference pageRef = new PageReference(page);
		if (existingPage.getFile() != null && existingPage.getFile().getDuration() > 0) {
			pageRef.setPageDisplayDurationOverride(existingPage.getFile().getDuration());
		}
		existingPlaylist.addPageReference(pageRef);

		return PlaylistDAO.save(existingPlaylist);
	}

	@POST
	@Path("/playlists/{playlistId}/playlists")
	@Consumes(MediaType.APPLICATION_JSON)
	public Playlist addPlaylist(@PathParam("playlistId") long playlistId, Playlist playlist) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		Playlist existingPlaylist2 = getExistingPlaylist(playlist.getId(), false);

		existingPlaylist.addPageReference(new PageReference(existingPlaylist2));

		return PlaylistDAO.save(existingPlaylist);
	}

	@POST
	@Path("/playlists/{playlistId}/files")
	@Consumes(MediaType.APPLICATION_JSON)
	public Playlist addFile(@PathParam("playlistId") long playlistId, File file) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		File existingFile = getExistingFile(file.getId(), false);

		PageReference pageRef = new PageReference(existingFile);
		if (existingFile.getDuration() > 0) {
			pageRef.setPageDisplayDurationOverride(existingFile.getDuration());
		}
		existingPlaylist.addPageReference(pageRef);

		return PlaylistDAO.save(existingPlaylist);
	}

	@PUT
	@Path("/playlists/{playlistId}/pagereferences/{pageRefId}")
	@Produces(MediaType.APPLICATION_JSON)
	public PageReference updatePageReference(@PathParam("pageRefId") long pageRefId, PageReference pageRef) {
		PageReference existingRef = new PageReferenceDAO().getById(pageRefId);
		if (existingRef == null) {
			throwNotFound();
		}
		if (PlaylistDAO.getPlaylistByPageReference(existingRef) == null) {
			throwUnauthorized();
		}

		existingRef.setPageDisplayDurationOverride(pageRef.getPageDisplayDurationOverride());
		existingRef.setRepeatEveryIteration(pageRef.getRepeatEveryIteration());
		existingRef.setRepeatEveryPage(pageRef.getRepeatEveryPage());

		return PageReferenceDAO.save(existingRef);
	}

	@POST
	@Path("/playlists/{playlistId}/copy")
	@Consumes(MediaType.APPLICATION_JSON)
	public Playlist copyPlaylist(@PathParam("playlistId") long playlistId, Playlist newPlaylist) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, false);

		Playlist copiedPlaylist = new Playlist(existingPlaylist);
		copiedPlaylist.setName(newPlaylist.getName());
		copiedPlaylist = createPlaylist(copiedPlaylist);

		// copy pages
		for (PageReference ref : existingPlaylist.getPageReferences()) {
			copiedPlaylist = addPage(copiedPlaylist.getId(), ref.getPage());
			updatePageReference(
					copiedPlaylist.getPageReferences().get(copiedPlaylist.getPageReferences().size() - 1).getId(), ref);
		}

		return copiedPlaylist;
	}

	@DELETE
	@Path("/playlists/{playlistId}")
	public Response deletePlaylist(@PathParam("playlistId") long playlistId) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		PlaylistReferences refs = getReferences(existingPlaylist.getId());
		if (refs.getForeignScreens() > 0 || !refs.getUserScreens().isEmpty()) {
			throwBadRequest("Playlist is in use in a screen.");
		}

		for (PageReference pageRef : existingPlaylist.getPageReferences()) {
			new PageReferenceDAO().deleteById(pageRef.getId());
		}
		for (Owner owner : existingPlaylist.getOwners()) {
			new OwnerDAO().deleteById(owner.getId());
		}

		return new PlaylistDAO().deleteById(playlistId) ? RESPONSE_OK : RESPONSE_BAD;
	}

	@DELETE
	@Path("/playlists/{playlistId}/pagereferences/{pageReferenceId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist deletePage(@PathParam("playlistId") long playlistId,
			@PathParam("pageReferenceId") long pageReferenceId) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);

		for (int i = 0; i < existingPlaylist.getPageReferences().size(); i++) {
			if (existingPlaylist.getPageReferences().get(i).getId() == pageReferenceId) {
				existingPlaylist.getPageReferences().remove(i);
				break;
			}
		}
		// re-index list
		for (int i = 0; i < existingPlaylist.getPageReferences().size(); i++) {
			existingPlaylist.getPageReferences().get(i).setPosition(i);
		}

		return PlaylistDAO.save(existingPlaylist);
	}

	@POST
	@Path("/playlists/{playlistId}/pagereferences/{pageReferenceId}/moveto/{idx}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist movePage(@PathParam("playlistId") long playlistId,
			@PathParam("pageReferenceId") long pageReferenceId, @PathParam("idx") int newPosition) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);

		// find current position
		int newIdx = -1;
		int oldIdx = -1;
		int oldPosition = -1;
		List<PageReference> pageReferences = existingPlaylist.getPageReferences();
		for (int i = 0; i < pageReferences.size(); i++) {
			if (pageReferences.get(i).getId() == pageReferenceId) {
				oldIdx = i;
				oldPosition = pageReferences.get(i).getPosition();
			}
			if (pageReferences.get(i).getPosition() == newPosition) {
				newIdx = i;
			}
		}
		if (oldIdx == newIdx) {
			return existingPlaylist;
		}
		if (oldIdx == -1 || newIdx == -1) {
			throwNotFound();
		}

		// create new list
		List<PageReference> newPageReferences = new ArrayList<>();
		if (oldPosition > newPosition) {
			if (newIdx > 0) {
				newPageReferences.addAll(pageReferences.subList(0, newIdx));
			}
			newPageReferences.add(pageReferences.get(oldIdx));
			pageReferences.remove(oldIdx);
			newPageReferences.addAll(pageReferences.subList(newIdx, pageReferences.size()));
		} else {
			PageReference tmp = pageReferences.get(oldIdx);
			pageReferences.remove(oldIdx);
			if (newIdx > 0) {
				newPageReferences.addAll(pageReferences.subList(0, newIdx));
			}
			newPageReferences.add(tmp);
			newPageReferences.addAll(pageReferences.subList(newIdx, pageReferences.size()));
		}

		// re-index list
		for (int i = 0; i < newPageReferences.size(); i++) {
			newPageReferences.get(i).setPosition(i);
		}
		existingPlaylist.setPageReferences(newPageReferences);

		return PlaylistDAO.save(existingPlaylist);
	}

	@GET
	@Path("/playlists/{playlistId}/references")
	@Produces(MediaType.APPLICATION_JSON)
	public PlaylistReferences getReferences(@PathParam("playlistId") long playlistId) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, false);

		if (!existingPlaylist.isListPublicly() && !this.isAdmin() && !inAdminOwners(existingPlaylist.getOwners())) {
			throwUnauthorized();
		}

		PlaylistReferences refs = new PlaylistReferences();

		// screens
		List<Screen> screens = ScreenDAO.getScreensByPlaylist(existingPlaylist);
		for (Screen screen : screens) {
			if (this.isAdmin() || inAdminOwners(screen.getOwners())) {
				refs.addUserScreen(screen);
			} else {
				refs.setForeignScreens(refs.getForeignScreens() + 1);
			}
		}

		// other playlists
		List<Playlist> playlists = PlaylistDAO.getPlaylistsByPlaylist(existingPlaylist);
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
	@Path("/playlists/{playlistId}/owners")
	@Consumes(MediaType.APPLICATION_JSON)
	public Playlist addOwners(@PathParam("playlistId") long playlistId, Owner[] ownerData) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		doAddOwners(existingPlaylist, ownerData);

		return PlaylistDAO.save(existingPlaylist);
	}

	@Override
	@DELETE
	@Path("/playlists/{playlistId}/owners/{ownerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Playlist deleteOwner(@PathParam("playlistId") long playlistId, @PathParam("ownerId") long ownerId) {
		Playlist existingPlaylist = getExistingPlaylist(playlistId, true);
		doDeleteOwner(existingPlaylist, ownerId);

		return PlaylistDAO.save(existingPlaylist);
	}

}
