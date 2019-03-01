package com.sap.primetime.dao.entities;

import java.util.List;

public interface OwnerSupport {
	public List<Owner> getOwners();

	public void setOwners(List<Owner> owners);

	public void addOwner(Owner owner);

}