package com.sap.primetime.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.User;

import io.swagger.annotations.Api;

@Api(value = "Users")
@Path("userservice")
public class UserService extends BasicService {
	@GET
	@Path("/user")
	@Produces(MediaType.APPLICATION_JSON)
	public User getCurrentUser() {
		return userUtil.getLoggedInDBUser(request);
	}

	@GET
	@Path("/users/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	public User getUser(@PathParam("userId") long userId) {
		return getExistingUser(userId);
	}

	@DELETE
	@Path("/users/{userId}")
	public Response deleteUser(@PathParam("userId") long userId) {
		if (!isDBAdmin()) {
			throwUnauthorized();
		}
		User existingUser = getExistingUser(userId);

		// remove references
		new OwnerDAO().deleteByField("user.id", existingUser.getId());

		return (new UserDAO().deleteByField("userId", existingUser.getUserId())) ? RESPONSE_OK : RESPONSE_BAD;
	}

	@POST
	@Path("/users/{userId}/impersonate")
	public Response impersonateUser(@PathParam("userId") long userId) {
		if (!isAdmin()) {
			throwUnauthorized();
		}
		User existingUser = getExistingUser(userId);

		userUtil.impersonateUser(request, existingUser.getUserId());

		return RESPONSE_OK;
	}

	@POST
	@Path("/stopimpersonation")
	public Response stopImpersonation() {
		if (!isAdmin()) {
			throwUnauthorized();
		}

		userUtil.stopImpersonation(request);

		return RESPONSE_OK;
	}

	@POST
	@Path("/logout")
	public Response logoutCurrentUser() {
		request.getSession().invalidate();
		userUtil.logOut(request);

		return RESPONSE_OK;
	}
}
