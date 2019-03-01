package com.sap.primetime.dao.entities;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import com.github.zafarkhaja.semver.Version;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigUtil;

@Table(name = "Screens")
@NamedQueries({
		@NamedQuery(name = Screen.QUERY_BYUSER, query = "SELECT s FROM Screen s INNER JOIN Owner o on (o IN (s.owners)) where o.user = :user order by LOWER(s.name)"),
		@NamedQuery(name = Screen.QUERY_BYKEY, query = "SELECT s FROM Screen s WHERE s.externalKey = :key"),
		@NamedQuery(name = Screen.QUERY_SCREENSHOTINSTANCES, query = "SELECT s FROM Screen s WHERE s.screenshotMode = true"),
		@NamedQuery(name = Screen.QUERY_ONBOARDINGS, query = "SELECT s FROM Screen s WHERE s.owners IS EMPTY order by s.externalKey"),
		@NamedQuery(name = Screen.QUERY_BYPLAYLIST, query = "SELECT s FROM Screen s WHERE s.playlist = :playlist and s.playlist != null"),
		@NamedQuery(name = Screen.QUERY_ACTIVE, query = "SELECT s FROM Screen s WHERE s.lastAlive > :thresholdDate") })
@Entity
public class Screen extends TenantAwareEntity implements OwnerSupport {
	// a bit longer than the regular statistics interval that kicks in in case there
	// is only one page
	public static final int DELAYED_THRESHOLD = 70;
	public static final String QUERY_BYUSER = "getScreensForUser";
	public static final String QUERY_BYKEY = "getScreenByKey";
	public static final String QUERY_ONBOARDINGS = "getOnboardings";
	public static final String QUERY_BYPLAYLIST = "getScreenByPlaylist";
	public static final String QUERY_ACTIVE = "getActiveScreens";
	public static final String QUERY_SCREENSHOTINSTANCES = "getScreenshotInstances";

	public enum ScreenType {
		TV
	}

	public enum AliveState {
		INITIAL, OK, DELAYED
	}

	public enum TransitionMode {
		INSTANT, SLIDE
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	@Index
	private String name;
	@Index
	private String externalKey;
	private String requestedOwner;
	private String location;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "PLAYLIST_ID")
	private Playlist playlist;
	private ScreenType screenType = ScreenType.TV;
	@Index
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "SCREEN_OWNER_ID")
	private List<Owner> owners = new ArrayList<>();

	private boolean showOwners = true;
	private boolean showHeader = true;
	private boolean showFooter = true;
	private boolean lowMemoryMode = false;
	private boolean screenshotMode = false;
	private TransitionMode transitionMode = TransitionMode.SLIDE;

	private int pageToShow = -1;
	private String remoteUser;

	@Temporal(TemporalType.TIMESTAMP)
	private Date lastAlive;
	@Temporal(TemporalType.TIMESTAMP)
	private Date lastContainerAlive;
	private int metric_resX;
	private int metric_resY;
	private long metric_currentPageId;
	private long metric_playlistTime;
	private long metric_applianceStarts;
	@Column(length = 4000)
	private String metric_applianceLog;
	@Temporal(TemporalType.TIMESTAMP)
	private Date metric_applianceStartDate;
	private String metric_applianceVersion;
	private String metric_browser;
	private String metric_os;
	private String metric_user;

	@Transient
	private AliveState aliveState = AliveState.INITIAL;
	private boolean appOutdated = false;

	public Screen() {
	}

	public Screen(String name) {
		this.name = name;
	}

	public Screen(Screen screen) {
		this.name = screen.getName();
		this.location = screen.getLocation();
		this.playlist = screen.getPlaylist();
		this.screenType = screen.getScreenType();
		this.showOwners = screen.isShowOwners();
		this.showHeader = screen.isShowHeader();
		this.showFooter = screen.isShowFooter();
		this.lowMemoryMode = screen.isLowMemoryMode();
		this.screenshotMode = screen.isScreenshotMode();
	}

	public Screen(String name, Playlist playlist) {
		this.name = name;
		this.playlist = playlist;
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

	public String getKey() {
		return externalKey;
	}

	public String getShortKey() {
		return externalKey != null ? externalKey.substring(0, 4) : null;
	}

	public void setKey(String key) {
		this.externalKey = key;
	}

	public String getRequestedOwner() {
		return requestedOwner;
	}

	public void setRequestedOwner(String requestedOwner) {
		this.requestedOwner = requestedOwner;
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	public void setPlaylist(Playlist playlist) {
		this.playlist = playlist;
	}

	public ScreenType getScreenType() {
		return screenType;
	}

	public void setScreenType(ScreenType screenType) {
		this.screenType = screenType;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
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

	public boolean isShowOwners() {
		return showOwners;
	}

	public void setShowOwners(boolean showOwners) {
		this.showOwners = showOwners;
	}

	public boolean isShowHeader() {
		return showHeader;
	}

	public void setShowHeader(boolean showHeader) {
		this.showHeader = showHeader;
	}

	public boolean isShowFooter() {
		return showFooter;
	}

	public void setShowFooter(boolean showFooter) {
		this.showFooter = showFooter;
	}

	public boolean isLowMemoryMode() {
		return lowMemoryMode;
	}

	public void setLowMemoryMode(boolean lowMemoryMode) {
		this.lowMemoryMode = lowMemoryMode;
	}

	public boolean isScreenshotMode() {
		return screenshotMode;
	}

	public void setScreenshotMode(boolean screenshotMode) {
		this.screenshotMode = screenshotMode;
	}

	public TransitionMode getTransitionMode() {
		return transitionMode;
	}

	public void setTransitionMode(TransitionMode transitionMode) {
		this.transitionMode = transitionMode;
	}

	public int getPageToShow() {
		return pageToShow;
	}

	public void setPageToShow(int pageToShow) {
		this.pageToShow = pageToShow;
	}

	public String getRemoteUser() {
		return remoteUser;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	public Date getLastAlive() {
		return lastAlive;
	}

	public void setLastAlive(Date lastAlive) {
		this.lastAlive = lastAlive;
	}

	public Date getLastContainerAlive() {
		return lastContainerAlive;
	}

	public void setLastContainerAlive(Date lastContainerAlive) {
		this.lastContainerAlive = lastContainerAlive;
	}

	public int getMetric_resX() {
		return metric_resX;
	}

	public void setMetric_resX(int metric_resX) {
		this.metric_resX = metric_resX;
	}

	public int getMetric_resY() {
		return metric_resY;
	}

	public void setMetric_resY(int metric_resY) {
		this.metric_resY = metric_resY;
	}

	public long getMetric_currentPageId() {
		return metric_currentPageId;
	}

	public void setMetric_currentPageId(long metric_currentPageId) {
		this.metric_currentPageId = metric_currentPageId;
	}

	public long getMetric_playlistTime() {
		return metric_playlistTime;
	}

	public void setMetric_playlistTime(long metric_playlistTime) {
		this.metric_playlistTime = metric_playlistTime;
	}

	public long getMetric_applianceStarts() {
		return metric_applianceStarts;
	}

	public void setMetric_applianceStarts(long metric_applianceStarts) {
		this.metric_applianceStarts = metric_applianceStarts;
	}

	public String getMetric_applianceLog() {
		return metric_applianceLog;
	}

	public void setMetric_applianceLog(String metric_applianceLog) {
		this.metric_applianceLog = metric_applianceLog;
	}

	public Date getMetric_applianceStartDate() {
		return metric_applianceStartDate;
	}

	public void setMetric_applianceStartDate(Date metric_applianceStartDate) {
		this.metric_applianceStartDate = metric_applianceStartDate;
	}

	public String getMetric_applianceVersion() {
		return metric_applianceVersion;
	}

	public void setMetric_applianceVersion(String metric_applianceVersion) {
		this.metric_applianceVersion = metric_applianceVersion;
	}

	public String getMetric_browser() {
		return metric_browser;
	}

	public void setMetric_browser(String metric_browser) {
		this.metric_browser = metric_browser;
	}

	public String getMetric_os() {
		return metric_os;
	}

	public void setMetric_os(String metric_os) {
		this.metric_os = metric_os;
	}

	public String getMetric_user() {
		return metric_user;
	}

	public void setMetric_user(String metric_user) {
		this.metric_user = metric_user;
	}

	public AliveState getAliveState() {
		return aliveState;
	}

	public void setAliveState(AliveState aliveState) {
		this.aliveState = aliveState;
	}

	public boolean isAppOutdated() {
		return appOutdated;
	}

	public void setAppOutdated(boolean appOutdated) {
		this.appOutdated = appOutdated;
	}

	@PostLoad
	private void updateDynamicValues() {
		if (playlist != null) {
			for (PageReference pageRef : playlist.getPageReferences()) {
				if (pageRef.getId() == metric_currentPageId) {
					pageRef.setCurrentlyShown(true);
					break;
				}
			}
		}
		if (lastAlive != null) {
			long seconds = ChronoUnit.SECONDS.between(lastAlive.toInstant(), Instant.now());
			setAliveState(seconds < DELAYED_THRESHOLD ? AliveState.OK : AliveState.DELAYED);
		}

		// check for outdated app
		if (StringUtils.isNotEmpty(externalKey) && StringUtils.isNotEmpty(metric_applianceVersion)) {
			Version v1 = Version.valueOf(metric_applianceVersion);
			Version v2 = Version.valueOf(ConfigUtil.getProperty(Consts.APP, Consts.PROP_RECOMMENDEDAPPLETVAPPVERSION));

			setAppOutdated(v1.lessThan(v2));
		}
	}

}
