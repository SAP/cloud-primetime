package com.sap.primetime.testutil;

import java.security.Principal;

import com.sap.primetime.dao.entities.User;

public class PrincipalMock implements Principal {
    private User user;

    public PrincipalMock(User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getUserId();
    }

}
