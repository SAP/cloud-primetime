package com.sap.primetime.dao.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PostLoad;
import javax.persistence.Table;

@Table(name = "Owners")
@Entity
public class Owner extends TenantAwareEntity {
	public enum Role {
		ADMINISTRATOR, CONTENT_MANAGER
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "USER_ID")
	private User user;
	@Column(name = "\"ROLE\"")
	private Role role = Role.ADMINISTRATOR;
	private boolean contact = true;

	public Owner() {
	}

	public Owner(User user) {
		this.setUser(user);
	}

	public Owner(User user, Role role) {
		this.setUser(user);
		this.setRole(role);
	}

	public long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isContact() {
		return contact;
	}

	public void setContact(boolean contact) {
		this.contact = contact;
	}

	@PostLoad
	private void updateDynamicValues() {
		// FIXME: why is this needed, user should have executed this already
		user.updateDynamicValues();
	}

}
