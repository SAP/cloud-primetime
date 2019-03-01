package com.sap.primetime.dao.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.Index;

import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.PageReference.PageRefType;

@Table(name = "Playlists")
@NamedQueries({
		@NamedQuery(name = Playlist.QUERY_BYUSER, query = "SELECT distinct p FROM Playlist p INNER JOIN Owner o on (o IN (p.owners)) where o.user = :user order by LOWER(p.name)"),
		@NamedQuery(name = Playlist.QUERY_BYNAME, query = "SELECT p FROM Playlist p WHERE p.name = :name"),
		@NamedQuery(name = Playlist.QUERY_BYPAGE, query = "SELECT distinct p FROM Playlist p INNER JOIN PageReference pr on (pr IN (p.pageReferences)) where pr.page = :page"),
		@NamedQuery(name = Playlist.QUERY_BYPLAYLIST, query = "SELECT distinct p FROM Playlist p INNER JOIN PageReference pr on (pr IN (p.pageReferences)) where pr.playlist = :playlist"),
		@NamedQuery(name = Playlist.QUERY_BYPAGEREFERENCE, query = "SELECT p FROM Playlist p where :pageReference MEMBER OF p.pageReferences"),
		@NamedQuery(name = Playlist.QUERY_PUBLIC, query = "SELECT p FROM Playlist p WHERE p.listPublicly=true order by LOWER(p.name)"),
		@NamedQuery(name = Playlist.QUERY_CATALOG, query = "SELECT distinct p FROM Playlist p INNER JOIN Owner o on (o IN (p.owners)) WHERE (p.listPublicly=true OR o.user = :user) and p.id != :excludeId order by LOWER(p.name)"),
		@NamedQuery(name = Playlist.DELETE_BYPAGE, query = "DELETE FROM Playlist p WHERE p.name = :name") })
@Entity
public class Playlist extends TenantAwareEntity implements OwnerSupport {
	public static final String QUERY_BYUSER = "getPlaylistsForUser";
	public static final String QUERY_BYNAME = "getPlaylistByName";
	public static final String QUERY_BYPAGE = "getPlaylistsByPage";
	public static final String QUERY_BYPLAYLIST = "getPlaylistsByPlaylist";
	public static final String QUERY_BYPAGEREFERENCE = "getPlaylistsByPageReference";
	public static final String DELETE_BYPAGE = "deletePageReferencesByPage";
	public static final String QUERY_PUBLIC = "getPublicPlaylists";
	public static final String QUERY_CATALOG = "getPlaylistCatalog";

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;

	@Index
	private String name;
	private int pageDisplayDuration = 10;
	@Index
	private boolean listPublicly = false;
	@Index
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "PAGEREFERENCE_ID")
	@OrderBy("position ASC")
	private List<PageReference> pageReferences = new ArrayList<>();
	@Index
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "PLAYLIST_OWNER_ID")
	private List<Owner> owners = new ArrayList<>();
	@Transient
	private int overallDuration;

	public Playlist() {
	}

	public Playlist(String name) {
		this.name = name;
	}

	public Playlist(Playlist playlist) {
		this.name = playlist.getName();
		this.pageDisplayDuration = playlist.getPageDisplayDuration();
		this.listPublicly = playlist.isListPublicly();
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPageDisplayDuration() {
		return pageDisplayDuration;
	}

	public void setPageDisplayDuration(int pageDisplayDuration) {
		this.pageDisplayDuration = pageDisplayDuration;
	}

	public boolean isListPublicly() {
		return listPublicly;
	}

	public void setListPublicly(boolean listPublicly) {
		this.listPublicly = listPublicly;
	}

	public List<PageReference> getPageReferences() {
		return pageReferences;
	}

	public void setPageReferences(List<PageReference> pageReferences) {
		this.pageReferences = pageReferences;
	}

	public void addPageReference(PageReference pageReference) {
		pageReference.setPosition(pageReferences.size());
		this.pageReferences.add(pageReference);
	}

	public List<Owner> getOwners() {
		return owners;
	}

	public void setOwners(List<Owner> owners) {
		this.owners = owners;
	}

	public void addOwner(Owner owner) {
		this.owners.add(owner);
	}

	@Override
	public String toString() {
		return "Playlist [Name=" + name + "]";
	}

	@PostLoad
	public void updateDynamicValues() {
		overallDuration = 0;
		for (PageReference page : pageReferences) {
			overallDuration += page.getPageDisplayDurationOverride() > 0 ? page.getPageDisplayDurationOverride()
					: pageDisplayDuration;
		}
	}

	public void expand() {
		List<PageReference> refs = new ArrayList<>();
		Stack<Long> stack = new Stack<>();
		stack.push(id);
		addRefs(refs, getPageReferences(), stack);
		refs = resolveRepetitions(refs);
		setPageReferences(refs);
		updateDynamicValues();
	}

	private void addRefs(List<PageReference> finalRefs, List<PageReference> refs, Stack<Long> stack) {
		for (PageReference pageReference : refs) {
			if (pageReference.getRefType() == PageRefType.PLAYLIST && pageReference.getPlaylist() != null) {
				// recursion check
				if (!stack.contains(pageReference.getPlaylist().getId())) {
					stack.push(pageReference.getPlaylist().getId());
					addRefs(finalRefs, pageReference.getPlaylist().getPageReferences(), stack);
					stack.pop();
				}
			} else if (pageReference.getRefType() == PageRefType.FILE && pageReference.getFile() != null) {
				// create a virtual page
				File file = pageReference.getFile();
				Page page = new Page(file.getName());
				page.setOwners(file.getOwners());
				page.setFile(file);
				switch (file.getFileType()) {
				case "mp4":
					page.setPageType(PageType.MOVIE);
					break;
				case "pdf":
					page.setPageType(PageType.PDF);
					break;
				default:
					page.setPageType(PageType.IMAGE);
				}
				page.updateDynamicValues();

				pageReference.setRefType(PageRefType.PAGE);
				pageReference.setPage(page);
				finalRefs.add(pageReference);
			} else {
				finalRefs.add(pageReference);
			}
		}
	}

	private List<PageReference> resolveRepetitions(List<PageReference> refs) {
		List<PageReference> result = new ArrayList<>();
		Map<Integer, Integer> iRepeat = new HashMap<>();
		Map<Integer, Integer> pRepeat = new HashMap<>();
		Map<Integer, Boolean> pRepeatInitialDone = new HashMap<>();
		int maxIRepeat = 1;

		// collect repetitions
		for (PageReference ref : refs) {
			if (ref.getRepeatEveryIteration() > 1) {
				iRepeat.put(ref.getPosition(), ref.getRepeatEveryIteration());
			}
			if (ref.getRepeatEveryPage() > 1) {
				pRepeat.put(ref.getPosition(), ref.getRepeatEveryPage());
			}
		}
		for (Integer value : iRepeat.values()) {
			maxIRepeat *= value;
		}

		// create max series length
		for (int i = 0; i < maxIRepeat; i++) {
			for (PageReference ref : refs) {
				if ((!iRepeat.containsKey(ref.getPosition()) || i % iRepeat.get(ref.getPosition()) == 0)
						&& (!pRepeat.containsKey(ref.getPosition())
								|| !pRepeatInitialDone.containsKey(ref.getPosition())
								|| !pRepeatInitialDone.get(ref.getPosition()))) {
					result.add(ref);
					pRepeatInitialDone.put(ref.getPosition(), true);
				}
			}
		}

		// insert page repetitions
		for (Integer pos : pRepeat.keySet()) {
			result = insertPageRepetitions(result, pos, pRepeat.get(pos));
		}

		return result;
	}

	private List<PageReference> insertPageRepetitions(List<PageReference> refs, int pos, int pattern) {
		List<PageReference> result = new ArrayList<>();
		PageReference templateRef = null;
		int counter = 1;

		for (PageReference ref : refs) {
			if (templateRef != null) {
				if (counter % pattern == 0) {
					result.add(templateRef);
					counter += 1;
				}
				counter += 1;
			}

			if (templateRef == null && ref.getPosition() == pos) {
				templateRef = ref;
			}
			result.add(ref);
		}

		return result;
	}

	public void breakRecursion() {
		if (getPageReferences() != null) {
			for (PageReference level1Ref : getPageReferences()) {
				if (level1Ref.getPlaylist() != null && level1Ref.getPlaylist().getPageReferences() != null) {
					for (PageReference level2Ref : level1Ref.getPlaylist().getPageReferences()) {
						// FIXME: this should be enough, next lines just for workaround
						// level2Ref.setPlaylist(null);
						if (level2Ref.getPlaylist() != null && level2Ref.getPlaylist().getPageReferences() != null) {
							for (PageReference level3Ref : level2Ref.getPlaylist().getPageReferences()) {
								level3Ref.setPlaylist(null);
							}
						}
					}
				}
			}
		}
	}

}
