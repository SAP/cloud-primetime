package com.sap.primetime.dao.entities;

import java.io.Serializable;

public class ConfigPK implements Serializable {
	private static final long serialVersionUID = 5730873049224049200L;
	private String paramGroup;
	private String paramKey;

	public ConfigPK() {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paramGroup == null) ? 0 : paramGroup.hashCode());
		result = prime * result + ((paramKey == null) ? 0 : paramKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ConfigPK other = (ConfigPK) obj;
		if (paramGroup == null) {
			if (other.paramGroup != null) {
				return false;
			}
		} else if (!paramGroup.equals(other.paramGroup)) {
			return false;
		}
		if (paramKey == null) {
			if (other.paramKey != null) {
				return false;
			}
		} else if (!paramKey.equals(other.paramKey)) {
			return false;
		}
		return true;
	}

}
