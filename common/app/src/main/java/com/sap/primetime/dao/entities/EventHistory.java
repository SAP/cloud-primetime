package com.sap.primetime.dao.entities;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.Index;

@Table(name = "EventHistory")
@Entity
public class EventHistory extends TenantAwareEntity {
	public enum EventType {
		ScreenAlive
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private long id;
	@Index
	private long entityId;
	private EventType eventType = EventType.ScreenAlive;
	@Temporal(TemporalType.TIMESTAMP)
	private Date eventDate;

	public EventHistory() {
	}

	public EventHistory(long entityId, Date eventDate) {
		this.entityId = entityId;
		this.eventDate = eventDate;
	}

	public long getId() {
		return id;
	}

	public long getEntityId() {
		return entityId;
	}

	public void setEntityId(long entityId) {
		this.entityId = entityId;
	}

	public EventType getEventType() {
		return eventType;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public Date getEventDate() {
		return eventDate;
	}

	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}

}
