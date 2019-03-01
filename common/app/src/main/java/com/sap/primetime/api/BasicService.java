package com.sap.primetime.api;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.primetime.adapters.DocumentAdapter;
import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.PlaylistDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.UserDAO;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Owner.Role;
import com.sap.primetime.dao.entities.OwnerSupport;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.PageReference;
import com.sap.primetime.dao.entities.Playlist;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.SecurityUtil;
import com.sap.primetime.util.UploadHandler;
import com.sap.primetime.util.UserUtil;
import com.sap.primetime.util.configuration.ConfigUtil;

/**
 * Contains helper functionalities, used by its subclasses for communication
 * between the server and the client through restful services.
 */
public abstract class BasicService {
	private static final Logger logger = LoggerFactory.getLogger(BasicService.class);

	protected static final Response RESPONSE_OK = Response.ok().build();
	protected static final Response RESPONSE_BAD = Response.status(Status.BAD_REQUEST).build();
	protected static final Response RESPONSE_NOTFOUND = Response.status(Status.NOT_FOUND).build();

	protected UserUtil userUtil = UserUtil.getInstance();

	protected static final Map<String, String> mimeTypes;
	static {
		mimeTypes = new HashMap<>();
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("jpeg", "image/jpeg");
		mimeTypes.put("png", "image/png");
		mimeTypes.put("gif", "image/gif");
		mimeTypes.put("ico", "image/x-icon");
		mimeTypes.put("svg", "image/svg+xml");
		mimeTypes.put("bmp", "image/bmp");
		mimeTypes.put("mp4", "video/mp4");
		mimeTypes.put("pdf", "application/pdf");
		mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		mimeTypes.put("htm", "text/html");
		mimeTypes.put("html", "text/html");
		mimeTypes.put("zip", "application/zip");
	}

	@Context
	protected HttpServletRequest request;

	@Context
	protected HttpServletResponse response;

	@Context
	protected ServletContext context;

	protected String sanitize(String text) {
		return SecurityUtil.textOnly(StringUtils.trim(text));
	}

	protected boolean isAdmin() {
		return userUtil.isAdmin(request);
	}

	protected boolean isDBAdmin() {
		return userUtil.isDBAdmin(request);
	}

	protected boolean inAdminOwners(List<Owner> owners) {
		return inOwners(owners, Role.ADMINISTRATOR, null);
	}

	protected boolean inAdminOwners(List<Owner> owners, String userId) {
		return inOwners(owners, Role.ADMINISTRATOR, userId);
	}

	protected boolean inContentOwners(List<Owner> owners) {
		return inOwners(owners, Role.CONTENT_MANAGER, null);
	}

	protected boolean inContentOwners(List<Owner> owners, String userId) {
		return inOwners(owners, Role.CONTENT_MANAGER, userId);
	}

	private boolean inOwners(List<Owner> owners, Role role, String userId) {
		if (userId == null) {
			userId = userUtil.getLoggedInUser(request).getUserId();
		} else if (!isAdmin() && !userId.equals(userUtil.getLoggedInUser(request).getUserId())) {
			throwUnauthorized();
		}

		for (Owner owner : owners) {
			if (role == owner.getRole() && userId.equals(owner.getUser().getUserId())) {
				return true;
			}
		}

		return false;
	}

	protected User getForeignUser(String userId) {
		User user = null;
		if (userId == null) {
			user = userUtil.getLoggedInDBUser(request);
		} else if (isAdmin() || userId.equals(userUtil.getLoggedInUser(request).getUserId())) {
			user = userUtil.getDBUser(userId);
		} else {
			throwUnauthorized();
		}

		return user;
	}

	protected void throwNotFound() {
		throwNotFound("Resource could not be found.");
	}

	protected void throwNotFound(String text) {
		throwError(Status.NOT_FOUND, text);
	}

	protected void throwUnauthorized() {
		throwUnauthorized("You do not have the permissions to perform this action.");
	}

	protected void throwUnauthorized(String text) {
		throwError(Status.UNAUTHORIZED, text);
	}

	protected void throwBadRequest() {
		throwBadRequest("The request could not be handled. Check the parameters.");
	}

	protected void throwBadRequest(String text) {
		throwError(Status.BAD_REQUEST, text);
	}

	protected void throwError(Status status, String text) {
		Response result = Response.status(status).entity(text).type(MediaType.TEXT_PLAIN).build();

		throw new WebApplicationException(new IOException(text), result);
	}

	protected File createFile(String fileName, String fileType, String key, long size) {
		File file = new File(fileName, size);
		file.setFileType(fileType);
		file.setFileKey(key);

		User user = userUtil.getLoggedInDBUser(request);
		Owner owner = new Owner(user);
		owner.setContact(true);
		file.addOwner(owner);

		return FileDAO.saveNew(file);
	}

	protected BufferedImage resize(BufferedImage img, int width, int height) {
		Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resized.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return resized;
	}

	protected BufferedImage rotate(BufferedImage img, double degrees) {
		double radians = Math.toRadians(degrees);

		int srcWidth = img.getWidth();
		int srcHeight = img.getHeight();

		// New image dimensions
		double sin = Math.abs(Math.sin(radians));
		double cos = Math.abs(Math.cos(radians));
		int targetWidth = (int) Math.floor(srcWidth * cos + srcHeight * sin);
		int targetHeight = (int) Math.floor(srcHeight * cos + srcWidth * sin);

		// Rotate
		BufferedImage result = new BufferedImage(targetWidth, targetHeight, img.getType());
		Graphics2D g = result.createGraphics();
		g.translate((targetWidth - srcWidth) / 2, (targetHeight - srcHeight) / 2);
		g.rotate(radians, srcWidth / 2, srcHeight / 2);
		g.drawRenderedImage(img, null);

		return result;
	}

	protected ByteArrayOutputStream getZipContent(InputStream stream, String path) {
		try (BufferedInputStream bis = new BufferedInputStream(stream);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
			ZipInputStream zin = new ZipInputStream(bis);
			ZipEntry ze = null;

			while ((ze = zin.getNextEntry()) != null) {
				if (ze.getName().equals(path)) {
					byte[] buffer = new byte[9000];
					int len;
					while ((len = zin.read(buffer)) != -1) {
						bos.write(buffer, 0, len);
					}
					return bos;
				}
			}
		} catch (IOException e) {
			logger.error("Could not extract ZIP file.", e);
		}

		return null;
	}

	protected void removePlaylistMetrics(Playlist playlist) {
		for (PageReference pageRef : playlist.getPageReferences()) {
			pageRef.setCurrentlyShown(false);
			pageRef.setStats_showTime(0);
			pageRef.setStats_viewCount(0);
			pageRef.setDateModified(null);
			if (pageRef.getPage() != null) {
				pageRef.getPage().setStats_showTime(0);
				pageRef.getPage().setStats_viewCount(0);
				pageRef.getPage().setDateModified(null);
			}
		}
	}

	protected DocumentAdapter getECM() {
		return Consts.injector.getInstance(DocumentAdapter.class);
	}

	protected Page getExistingPage(long pageId, boolean checkOwnership) {
		Page result = new PageDAO().getById(pageId);
		if (result == null) {
			throwNotFound();
		}
		if (checkOwnership && !this.isAdmin() && !inAdminOwners(result.getOwners())) {
			throwUnauthorized();
		}

		return result;
	}

	protected File getExistingFile(long fileId, boolean checkOwnership) {
		File result = new FileDAO().getById(fileId);
		if (result == null) {
			throwNotFound();
		}
		if (checkOwnership && !isAdmin() && !inAdminOwners(result.getOwners())) {
			throwUnauthorized();
		}

		return result;
	}

	protected Playlist getExistingPlaylist(long playlistId, boolean checkOwnership) {
		Playlist result = new PlaylistDAO().getById(playlistId);
		if (result == null) {
			throwNotFound();
		}
		if (checkOwnership && !this.isAdmin() && !inAdminOwners(result.getOwners())) {
			throwUnauthorized();
		}

		return result;
	}

	protected Screen getExistingScreen(long screenId, boolean checkOwnership) {
		Screen result = new ScreenDAO().getById(screenId);
		if (result == null) {
			throwNotFound();
		}

		if (checkOwnership && !this.isAdmin() && !inAdminOwners(result.getOwners())) {
			throwUnauthorized();
		}

		return result;
	}

	protected User getExistingUser(long userId) {
		User result = new UserDAO().getById(userId);
		if (result == null) {
			throwNotFound();
		}

		return result;
	}

	protected void doAddOwners(OwnerSupport entity, Owner[] ownerData) {
		for (int i = 0; i < ownerData.length; i++) {
			String ownerUserId = ownerData[i].getUser().getUserId().toLowerCase(Locale.ENGLISH);
			String userPattern = ConfigUtil.getProperty(Consts.APP, Consts.PROP_USERIDPATTERN);
			if (userPattern != null && !ownerUserId.matches(userPattern)) {
				throwBadRequest("User Id does not fit expected format.");
			}

			for (Owner owner : entity.getOwners()) {
				if (owner.getUser().getUserId().equalsIgnoreCase(ownerUserId)) {
					throwError(Status.CONFLICT, "Owner already exists.");
				}
			}

			User existingUser = userUtil.getDBUser(ownerData[i].getUser().getUserId());
			entity.addOwner(new Owner(existingUser, ownerData[i].getRole()));
		}
	}

	protected void doDeleteOwner(OwnerSupport entity, long ownerId) {
		if (entity.getOwners().size() == 1) {
			throwBadRequest("At least one owner must remain.");
		}

		for (int i = 0; i < entity.getOwners().size(); i++) {
			if (entity.getOwners().get(i).getId() == ownerId) {
				entity.getOwners().remove(i);
				break;
			}
		}
	}

	protected File doUploadScreenshot(String key, String namePrefix, int width, int height) {
		File file = null;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			FileItem document = UploadHandler.getUploadedDocument(request);
			String fileType = "png";
			String fileKey = key;
			String fileName = namePrefix + key;

			// resize image
			BufferedImage image = ImageIO.read(document.getInputStream());
			BufferedImage resizedImage = resize(image, width, height);
			ImageIO.write(resizedImage, fileType, bos);
			long fileSize = bos.size();

			getECM().uploadDocument(fileKey, bos.toByteArray(), mimeTypes.get(fileType));

			file = new FileDAO().getSingleByField("name", fileName);
			if (file == null) {
				File newFile = new File(fileName, fileSize);
				newFile.setFileKey(fileKey);
				newFile.setFileType(fileType);
				file = FileDAO.saveNew(newFile);
			} else {
				file.setSize(fileSize);
				file.setFileType(fileType);

				// force update since screenshot might be 100% identical which will then not
				// trigger a persist
				file.setDateModified(new Date());
				file = FileDAO.save(file);
			}

			document.delete();
		} catch (FileUploadException e) {
			throwBadRequest("Screenshot could not be uploaded.");
		} catch (CmisNameConstraintViolationException e) {
			throwBadRequest("Screenshot naming exception.");
		} catch (IOException e) {
			throwBadRequest("Screenshot could not be resized.");
		}

		return file;
	}

	protected String getServerUrl() {
		String result = ConfigUtil.getProperty(Consts.APP, Consts.PROP_APPURL);
		if (StringUtils.isAllBlank(result)) {
			try {
				URL url = new URL(request.getRequestURL().toString());
				result = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), null, null, null)
						.toString();
			} catch (MalformedURLException e) {
				logger.warn("Could not determine server URL.", e);
			} catch (URISyntaxException e) {
				logger.warn("Server URL could not be constructed.", e);
			}
		}

		return result;
	}
}
