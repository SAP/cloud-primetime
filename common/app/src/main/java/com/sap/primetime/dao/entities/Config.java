package com.sap.primetime.dao.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Table(name = "Configurations")
@NamedQueries({
		@NamedQuery(name = Config.QUERY_BYGROUP, query = "SELECT c FROM Config c WHERE c.paramGroup = :group order by c.paramGroup"),
		@NamedQuery(name = Config.QUERY_DELETE, query = "DELETE FROM Config c WHERE c.paramGroup = :group and c.paramKey = :key") })
@IdClass(ConfigPK.class)
@Entity
public class Config extends TenantAwareEntity {
	public static final String QUERY_BYGROUP = "getConfigByGroup";
	public static final String QUERY_DELETE = "deleteConfigById";

	@Id
	private String paramGroup;
	@Id
	private String paramKey;
	@Column(length = 4000)
	private String paramValue;

	public Config() {
	}

	public Config(String group, String key, String value) {
		this.paramGroup = group;
		this.paramKey = key;
		this.paramValue = value;
	}

	public String getParamGroup() {
		return paramGroup;
	}

	public void setParamGroup(String paramGroup) {
		this.paramGroup = paramGroup;
	}

	public String getParamKey() {
		return paramKey;
	}

	public void setParamKey(String paramKey) {
		this.paramKey = paramKey;
	}

	public String getParamValue() {
		return paramValue;
	}

	public void setParamValue(String paramValue) {
		this.paramValue = paramValue;
	}

	@Override
	public String toString() {
		return "Config [paramGroup=" + paramGroup + ", paramKey=" + paramKey + ", paramValue=" + paramValue + "]";
	}

}
