package com.sap.primetime.dao.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.Index;

// TODO: evaluate benefits of an inheritance strategy, at least in Java, can go into same table in the end
@Table(name = "Pages")
@NamedQueries({
		@NamedQuery(name = Page.QUERY_BYUSER, query = "SELECT p FROM Page p INNER JOIN Owner o on (o IN (p.owners)) where o.user = :user order by LOWER(p.name)"),
		@NamedQuery(name = Page.QUERY_CATALOG, query = "SELECT p FROM Page p INNER JOIN Owner o on (o IN (p.owners)) WHERE p.listPublicly=true OR o.user = :user order by LOWER(p.name)"),
		@NamedQuery(name = Page.QUERY_BYFILEORWITHTEMPLATE, query = "SELECT p FROM Page p WHERE p.file = :file or p.pageType = com.sap.primetime.dao.entities.Page.PageType.TEMPLATE"),
		@NamedQuery(name = Page.QUERY_WITHOUTSCREENSHOT, query = "SELECT p FROM Page p WHERE p.autoScreenshot = null and p.pageType != com.sap.primetime.dao.entities.Page.PageType.PDF and p.pageType != com.sap.primetime.dao.entities.Page.PageType.IMAGE and p.pageType != com.sap.primetime.dao.entities.Page.PageType.MOVIE"),
		@NamedQuery(name = Page.QUERY_AUTOSCREENSHOT, query = "SELECT p FROM Page p where p.pageType != com.sap.primetime.dao.entities.Page.PageType.MOVIE order by p.autoScreenshot.dateModified"),
		@NamedQuery(name = Page.QUERY_ADVERTISED, query = "SELECT p FROM Page p WHERE p.listPublicly=true order by p.stats_viewCount desc") })
@Entity
public class Page extends TenantAwareEntity implements OwnerSupport {
	public static final String QUERY_BYUSER = "getPagesForUser";
	public static final String QUERY_CATALOG = "getPageCatalog";
	public static final String QUERY_ADVERTISED = "getAdvertisedPages";
	public static final String QUERY_BYFILEORWITHTEMPLATE = "getPagesByFile";
	public static final String QUERY_WITHOUTSCREENSHOT = "getPagesWithoutScreenshot";
	public static final String QUERY_AUTOSCREENSHOT = "getPagesForAutoScreenshot";

	public enum PageType {
		URL, URLGRID, PDF, TEXT, IMAGE, HTML, MOVIE, YOUTUBE, MEDIASHARE, TEMPLATE
	}

	// shared data between all pages
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	private String name;
	private String title;
	@Column(length = 4000)
	private String description;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "SCREENSHOT_ID")
	private File screenshot;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "AUTOSCREENSHOT_ID")
	private File autoScreenshot;
	@Index
	private boolean listPublicly = false;
	private int reloadInterval = 0;
	private PageType pageType = PageType.URL;
	@Index
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "PAGE_OWNER_ID")
	private List<Owner> owners = new ArrayList<>();
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateStart;
	@Temporal(TemporalType.TIMESTAMP)
	private Date dateEnd;
	private long stats_viewCount = 0;
	private long stats_showTime = 0;

	// page type: URL, URL GRID
	@Column(length = 4000)
	private String url;

	// page type: URL GRID
	private int gridX = 2;
	private int gridY = 2;
	private int gridRatio = 0;

	// page type: IMAGE, PDF, MOVIE
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "FILE_ID")
	private File file;

	// page type: TEMPLATE
	@Column(length = 4000)
	private String templateValues;

	// page type: MEDIASHARE
	private boolean mediasharePrivate = false;

	// page type: TEXT, YOUTUBE, MEDIASHARE
	@Column(length = 4000)
	private String text;

	// page type: PDF
	private int page = 1;

	@Transient
	private String ownersDisplayText;
	@Transient
	private String screenshotUrl;

	public Page() {
	}

	public Page(String name) {
		this.name = name;
	}

	public Page(Page page) {
		this.name = page.getName();
		this.title = page.getTitle();
		this.description = page.getDescription();
		this.url = page.getUrl();
		this.listPublicly = page.isListPublicly();
		this.reloadInterval = page.getReloadInterval();
		this.pageType = page.getPageType();
		this.text = page.getText();
		this.page = page.getPage();
		this.gridX = page.getGridX();
		this.gridY = page.getGridY();
		this.gridRatio = page.getGridRatio();
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getTemplateValues() {
		return templateValues;
	}

	public void setTemplateValues(String templateValues) {
		this.templateValues = templateValues;
	}

	public File getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(File screenshot) {
		this.screenshot = screenshot;
	}

	public File getAutoScreenshot() {
		return autoScreenshot;
	}

	public void setAutoScreenshot(File autoScreenshot) {
		this.autoScreenshot = autoScreenshot;
	}

	public boolean isMediasharePrivate() {
		return mediasharePrivate;
	}

	public void setMediasharePrivate(boolean mediasharePrivate) {
		this.mediasharePrivate = mediasharePrivate;
	}

	public boolean isListPublicly() {
		return listPublicly;
	}

	public void setListPublicly(boolean listPublicly) {
		this.listPublicly = listPublicly;
	}

	public int getReloadInterval() {
		return reloadInterval;
	}

	public void setReloadInterval(int reloadInterval) {
		this.reloadInterval = reloadInterval;
	}

	public PageType getPageType() {
		return pageType;
	}

	public void setPageType(PageType pageType) {
		this.pageType = pageType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getGridX() {
		return gridX;
	}

	public void setGridX(int gridX) {
		this.gridX = gridX;
	}

	public int getGridY() {
		return gridY;
	}

	public void setGridY(int gridY) {
		this.gridY = gridY;
	}

	public int getGridRatio() {
		return gridRatio;
	}

	public void setGridRatio(int gridRatio) {
		this.gridRatio = gridRatio;
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

	public Date getDateStart() {
		return dateStart;
	}

	public void setDateStart(Date dateStart) {
		this.dateStart = dateStart;
	}

	public Date getDateEnd() {
		return dateEnd;
	}

	public void setDateEnd(Date dateEnd) {
		this.dateEnd = dateEnd;
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

	public String getOwnersDisplayText() {
		return ownersDisplayText;
	}

	@Override
	public String toString() {
		return "Page [name=" + name + "]";
	}

	@PostLoad
	public void updateDynamicValues() {
		List<String> temp = new ArrayList<>();
		ownersDisplayText = "";
		for (int i = 0; i < owners.size(); i++) {
			if (owners.get(i).isContact()) {
				owners.get(i).getUser().updateDynamicValues();
				temp.add(owners.get(i).getUser().getDisplayName());
			}
		}
		ownersDisplayText = StringUtils.join(temp, ", ");

		// set preview automatically for supported page types
		if (file != null && (pageType == PageType.IMAGE || (pageType == PageType.MOVIE && screenshot == null)
				|| pageType == PageType.PDF)) {
			screenshotUrl = "/s/api/fileservice/files/" + file.getId() + "/content?thumbnail=true&page=" + page;
		} else if (screenshot != null) {
			screenshotUrl = "/s/api/fileservice/files/" + screenshot.getId() + "/content";
		} else if (autoScreenshot != null) {
			screenshotUrl = "/s/api/fileservice/files/" + autoScreenshot.getId() + "/content";
		} else {
			screenshotUrl = "/ui/img/missing_screenshot.png";
		}
	}

}
