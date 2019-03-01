package com.sap.primetime.dao.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "PageReferences")
@Entity
@NamedQuery(name = PageReference.QUERY_DELETEBYPAGE, query = "DELETE from PageReference p where p.page = :page")
public class PageReference extends TenantAwareEntity {
	public static final String QUERY_DELETEBYPAGE = "deleteByPage";

	public enum PageRefType {
		PAGE, PLAYLIST, FILE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	private PageRefType refType = PageRefType.PAGE;

	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "PAGE_ID")
	private Page page;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "PLAYLIST_ID")
	private Playlist playlist;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "FILE_ID")
	private File file;
	private int position = 0;
	private int pageDisplayDurationOverride = 0;
	private boolean visible = true;
	private int repeatEveryIteration = 1;
	private int repeatEveryPage = 0;

	// automatically filled
	private long stats_viewCount = 0;
	private long stats_showTime = 0;
	@Transient
	private boolean customDuration;
	@Transient
	private boolean currentlyShown;

	public PageReference() {
	}

	public PageReference(Page page) {
		this.page = page;
		this.refType = PageRefType.PAGE;
	}

	public PageReference(Playlist playlist) {
		this.playlist = playlist;
		this.refType = PageRefType.PLAYLIST;
	}

	public PageReference(File file) {
		this.file = file;
		this.refType = PageRefType.FILE;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public PageRefType getRefType() {
		return refType;
	}

	public void setRefType(PageRefType refType) {
		this.refType = refType;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPageDisplayDurationOverride() {
		return pageDisplayDurationOverride;
	}

	public void setPageDisplayDurationOverride(int pageDisplayDurationOverride) {
		this.pageDisplayDurationOverride = pageDisplayDurationOverride;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getRepeatEveryIteration() {
		return repeatEveryIteration;
	}

	public void setRepeatEveryIteration(int repeatEveryIteration) {
		this.repeatEveryIteration = repeatEveryIteration;
	}

	public int getRepeatEveryPage() {
		return repeatEveryPage;
	}

	public void setRepeatEveryPage(int repeatEveryPage) {
		this.repeatEveryPage = repeatEveryPage;
	}

	public long getStats_viewCount() {
		return stats_viewCount;
	}

	public void setStats_viewCount(long stats_viewCount) {
		this.stats_viewCount = stats_viewCount;
	}

	public long getStats_showTime() {
		return stats_showTime;
	}

	public void setStats_showTime(long stats_showTime) {
		this.stats_showTime = stats_showTime;
	}

	public void setCurrentlyShown(boolean currentlyShown) {
		this.currentlyShown = currentlyShown;
	}

	@PostLoad
	private void updateDynamicValues() {
		customDuration = pageDisplayDurationOverride > 0;
	}

	@Override
	public String toString() {
		return "PageReference [Page=" + page + ", Playlist=" + playlist + "]";
	}

}
