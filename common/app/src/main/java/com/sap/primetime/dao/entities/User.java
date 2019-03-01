package com.sap.primetime.dao.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.Index;

import com.sap.primetime.util.Consts;
import com.sap.primetime.util.configuration.ConfigUtil;

@Table(name = "Users")
@NamedQuery(name = User.QUERY_BYUSERID, query = "SELECT u FROM User u WHERE LOWER(u.userId) = LOWER(:userId)")
@Entity
public class User extends TenantAwareEntity {
	public static final String QUERY_BYUSERID = "getUserByUserId";

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	@Index
	private String userId;
	private String firstName;
	private String lastName;
	private String email;

	@Transient
	private String imageLink;
	@Transient
	private String profileLink;
	@Transient
	private String displayName;
	@Transient
	private String fullName;

	public User() {
	}

	public long getId() {
		return id;
	}

	public User(String userId) {
		this.userId = userId;

		updateDynamicValues();
	}

	public User(String userId, String email) {
		this.userId = userId;
		this.email = email;

		updateDynamicValues();
	}

	public User(String userId, String email, String firstName, String lastName) {
		this.userId = userId;
		this.email = email;
		this.firstName = firstName;
		this.lastName = lastName;

		updateDynamicValues();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getImageLink() {
		return imageLink;
	}

	public void setImageLink(String imageLink) {
		this.imageLink = imageLink;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFullName() {
		return fullName;
	}

	@PostLoad
	public void updateDynamicValues() {
		fullName = (firstName != null && lastName != null) ? firstName + " " + lastName : null;
		displayName = StringUtils.isNotBlank(getFullName()) ? getFullName() : userId;

		String avatarUrl = ConfigUtil.getProperty(Consts.APP, Consts.PROP_AVATARURL);
		imageLink = avatarUrl == null ? null : replacePlaceholders(avatarUrl);

		String profileUrl = ConfigUtil.getProperty(Consts.APP, Consts.PROP_PROFILEURL);
		profileLink = profileUrl == null ? null : replacePlaceholders(profileUrl);
	}

	private String replacePlaceholders(String original) {
		return original.replaceAll("%userId%", userId)
				.replaceAll("%firstName%", firstName != null ? firstName.toLowerCase() : "")
				.replaceAll("%lastName%", lastName != null ? lastName.toLowerCase() : "");
	}
}
