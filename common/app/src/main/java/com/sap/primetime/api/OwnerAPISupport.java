package com.sap.primetime.api;

import com.sap.primetime.dao.entities.Owner;

public interface OwnerAPISupport<T> {

	T addOwners(long id, Owner[] ownerData);

	@SuppressWarnings("unused")
	default T updateOwner(long id, long ownerId, Owner owner) {
		return null;
	}

	T deleteOwner(long id, long ownerId);

}