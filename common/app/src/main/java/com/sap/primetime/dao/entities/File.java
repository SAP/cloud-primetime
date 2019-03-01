package com.sap.primetime.dao.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.Index;

@Table(name = "Files")
@NamedQuery(name = File.QUERY_BYUSER, query = "SELECT f FROM File f INNER JOIN Owner o on (o IN (f.owners)) where o.user = :user order by LOWER(f.name)")
@Entity
public class File extends TenantAwareEntity implements OwnerSupport {
	public static final String QUERY_BYUSER = "getFilesForUser";

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	private String name;
	private String fileKey;
	private long size;
	private String fileType;
	@Index
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "FILE_OWNER_ID")
	private List<Owner> owners = new ArrayList<>();

	// file type: PDF
	private long pageCount;

	// file type: IMAGE, MOVIE
	private int width;
	private int height;

	// file type: MOVIE
	private int duration;

	// file type: TEMPLATE
	private String screenshotPath;
	private String version;
	@Column(length = 4000)
	private String description;
	@Column(length = 4000)
	private String parameters;
	@Column(length = 4000)
	private String errorState;
	@Index
	private boolean listPublicly = false;

	@Transient
	private String screenshotUrl;

	public File() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public File(String name) {
		this.name = name;
	}

	public File(String name, long size) {
		this.name = name;
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFileKey() {
		return fileKey;
	}

	public void setFileKey(String fileKey) {
		this.fileKey = fileKey;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType.replace("jpeg", "jpg").replace("zip", "template");
	}

	public long getPageCount() {
		return pageCount;
	}

	public void setPageCount(long pageCount) {
		this.pageCount = pageCount;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getScreenshotPath() {
		return screenshotPath;
	}

	public void setScreenshotPath(String screenshotPath) {
		this.screenshotPath = screenshotPath;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getErrorState() {
		return errorState;
	}

	public void setErrorState(String errorState) {
		this.errorState = errorState;
	}

	public boolean isListPublicly() {
		return listPublicly;
	}

	public void setListPublicly(boolean listPublicly) {
		this.listPublicly = listPublicly;
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

	@PostLoad
	public void updateDynamicValues() {
		screenshotUrl = "/s/api/fileservice/files/" + id + "/content?thumbnail=true";
	}

}
