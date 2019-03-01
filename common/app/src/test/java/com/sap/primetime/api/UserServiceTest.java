package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Locale;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;

import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.HttpServletRequestMock;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.util.UserUtil;

public class UserServiceTest extends PrimeTimeTest {
	private UserService userService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		userService = spy(new UserService());
		userService.request = requestMock;
	}

	@Test
	public void testGetCurrentUser() throws Exception {
		User user = userService.getCurrentUser();
		assertNotNull(user);
		assertEquals(user.getUserId(), simpleUser.getUserId());
		assertNotNull(user.getImageLink());
		assertFalse(user.getImageLink().contains(("%")));
	}

	@Test
	public void testUserIdChange() throws Exception {
		assertEquals(0, new UserDAO().getCount());

		assertEquals(simpleUser.getUserId(), userService.getCurrentUser().getUserId());
		assertEquals(1, new UserDAO().getCount());

		// simulate same user with different capitalization
		requestMock = new HttpServletRequestMock(new User(simpleUser.getUserId().toUpperCase(Locale.ENGLISH),
				simpleUser.getEmail().toUpperCase(Locale.ENGLISH)));
		userService.request = requestMock;

		assertEquals(simpleUser.getUserId().toUpperCase(), userService.getCurrentUser().getUserId());
		assertEquals(1, new UserDAO().getCount());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingUser() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(userService).isAdmin();
		userService.getUser(-1);
	}

	public void testGetOtherUserAsNormalUser() throws Exception {
		simpleUser2 = UserUtil.getInstance().getDBUser(simpleUser2.getUserId());

		assertEquals(simpleUser2.getUserId(), userService.getUser(simpleUser2.getId()).getUserId());
	}

	@Test
	public void testGetOtherUserAsAdmin() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(userService).isAdmin();
		simpleUser2 = UserUtil.getInstance().getDBUser(simpleUser2.getUserId());

		assertEquals(simpleUser2.getUserId(), userService.getUser(simpleUser2.getId()).getUserId());
	}

	@Test
	public void testGetUser() throws Exception {
		simpleUser = UserUtil.getInstance().getDBUser(simpleUser.getUserId());
		User user = userService.getUser(simpleUser.getId());
		assertNotNull(user);
		assertEquals(simpleUser.getUserId(), user.getUserId());
	}

	@Test
	public void testImpersonation() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(userService).isAdmin();
		doReturn(true).when(requestMock).isUserInRole("admin");

		simpleUser = UserUtil.getInstance().getDBUser(simpleUser.getUserId());
		simpleUser2 = UserUtil.getInstance().getDBUser(simpleUser2.getUserId());

		assertEquals(simpleUser.getId(), userService.getCurrentUser().getId());
		assertNull(UserUtil.getInstance().isImpersonating(requestMock));

		userService.impersonateUser(simpleUser2.getId());
		assertEquals(simpleUser2.getId(), userService.getCurrentUser().getId());
		assertEquals(simpleUser2.getUserId(), UserUtil.getInstance().isImpersonating(requestMock));

		userService.stopImpersonation();
		assertEquals(simpleUser.getId(), userService.getCurrentUser().getId());
		assertNull(UserUtil.getInstance().isImpersonating(requestMock));
	}

	@Test(expected = WebApplicationException.class)
	public void testNonAdminImpersonation() throws Exception {
		simpleUser = UserUtil.getInstance().getDBUser(simpleUser.getUserId());
		simpleUser2 = UserUtil.getInstance().getDBUser(simpleUser2.getUserId());

		userService.impersonateUser(simpleUser2.getId());
	}

}
