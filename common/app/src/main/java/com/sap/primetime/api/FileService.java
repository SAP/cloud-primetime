package com.sap.primetime.api;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.OwnerDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.FileReferences;
import com.sap.primetime.dao.entities.EventHistory;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.util.Consts;
import com.sap.primetime.util.HTTPRange;
import com.sap.primetime.util.TemplateHelper;
import com.sap.primetime.util.UploadHandler;

@Path("fileservice")
public class FileService extends BasicService implements OwnerAPISupport<File> {
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@GET
	@Path("/files")
	@Produces(MediaType.APPLICATION_JSON)
	public List<File> getFiles(@QueryParam("userId") String userId) {
		return FileDAO.getFilesForUser(getForeignUser(userId));
	}

	@GET
	@Path("/files/{fileId}")
	@Produces(MediaType.APPLICATION_JSON)
	public File getFile(@PathParam("fileId") long id) {
		return getExistingFile(id, false);
	}

	@PUT
	@Path("/files/{fileId}")
	@Produces(MediaType.APPLICATION_JSON)
	public File updateFile(@PathParam("fileId") long fileId, File fileData) {
		File existingFile = getExistingFile(fileId, true);

		existingFile.setName(fileData.getName());
		existingFile.setDescription(fileData.getDescription());
		existingFile.setListPublicly(fileData.isListPublicly());

		return FileDAO.save(existingFile);
	}

	@POST
	@Path("/files")
	public Response uploadFile() {
		String fileName = null;
		long fileSize = 0;
		FileItem document = null;
		byte[] content = null; // FIXME: this will kill the server for huge files, switch to streaming

		if (!ServletFileUpload.isMultipartContent(request)) {
			// plain upload
			fileSize = request.getContentLength();
			fileName = request.getHeader("filename");
			if (fileName == null) {
				throwBadRequest("Header 'filename' is missing in request.");
			}
			try {
				content = IOUtils.readFully(request.getInputStream(), request.getContentLength());
			} catch (IOException e) {
				logger.error("File could not be read.", e);
				return RESPONSE_BAD;
			}
		} else {
			// multipart/form encoded
			try {
				document = UploadHandler.getUploadedDocument(request);
			} catch (FileUploadException e) {
				logger.error("File could not be uploaded.", e);
				return RESPONSE_BAD;
			}
			fileName = document.getName();
			fileSize = document.getSize();
			content = document.get();
		}

		fileName = org.apache.commons.io.FilenameUtils.getName(fileName);
		String key = Math.abs(new Random().nextInt()) + "_" + fileName;
		if (!fileName.contains(".")) {
			if (document != null) {
				document.delete();
			}
			throwBadRequest("File type could not be determined.");
		}
		String fileType = FilenameUtils.getExtension(fileName).toLowerCase();
		if (!mimeTypes.containsKey(fileType)) {
			if (document != null) {
				document.delete();
			}
			throwBadRequest("Unsupported file type.");
		}
		try {
			getECM().uploadDocument(key, content, mimeTypes.get(fileType));
			fileName = FilenameUtils.getBaseName(fileName);
			File file = createFile(fileName, fileType, key, fileSize);

			addContentMetadata(file, new ByteArrayInputStream(content));

			if (document != null) {
				document.delete();
			}
		} catch (CmisNameConstraintViolationException e) {
			logger.error("Filename exception.", e);
			return RESPONSE_BAD;
		}
		deleteThumbnails(key);

		return RESPONSE_OK;
	}

	@POST
	@Path("/files/{fileId}/rotate")
	public Response rotateImage(@PathParam("fileId") long fileId) {
		File existingFile = getExistingFile(fileId, true);

		String[] supportedTypes = { "jpg", "png", "gif", "bmp" };
		if (!Arrays.asList(supportedTypes).contains(existingFile.getFileType())) {
			throwBadRequest("Unsupported file format.");
		}

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			BufferedImage img = ImageIO
					.read(getECM().getDocumentByName(existingFile.getFileKey()).getContentStream().getStream());
			img = rotate(img, 90);
			ImageIO.write(img, existingFile.getFileType(), bos);

			existingFile.setWidth(img.getWidth());
			existingFile.setHeight(img.getHeight());
			existingFile = FileDAO.save(existingFile);

			getECM().uploadDocument(existingFile.getFileKey(), bos.toByteArray(),
					mimeTypes.get(existingFile.getFileType()));
			deleteThumbnails(existingFile.getFileKey());
		} catch (IOException e) {
			logger.error("Image processing exception.", e);
			return RESPONSE_BAD;
		}

		return RESPONSE_OK;
	}

	@POST
	@Path("/files/{fileId}/content")
	public Response updateFileContent(@PathParam("fileId") long fileId) {
		File existingFile = getExistingFile(fileId, true);

		try {
			FileItem document = UploadHandler.getUploadedDocument(request);
			String fileName = FilenameUtils.getName(document.getName());
			if (!fileName.contains(".")) {
				document.delete();
				throwBadRequest("File type could not be determined.");
			}
			String fileType = FilenameUtils.getExtension(fileName).toLowerCase();
			if (!mimeTypes.containsKey(fileType)) {
				document.delete();
				throwBadRequest("Unsupported file type.");
			}

			getECM().uploadDocument(existingFile.getFileKey(), document.get(), mimeTypes.get(fileType));
			existingFile.setDateModified(new Date());
			existingFile.setSize(document.getSize());
			existingFile.setFileType(fileType);
			existingFile = FileDAO.save(existingFile);

			addContentMetadata(existingFile, document.getInputStream());

			deleteThumbnails(existingFile.getFileKey());
			document.delete();
		} catch (FileUploadException e) {
			logger.error("File could not be uploaded.", e);
			return RESPONSE_BAD;
		} catch (CmisNameConstraintViolationException e) {
			logger.error("Filename exception.", e);
			return RESPONSE_BAD;
		} catch (IOException e) {
			logger.error("File could not be read.", e);
			return RESPONSE_BAD;
		}

		return RESPONSE_OK;
	}

	private boolean addContentMetadata(File existingFile, InputStream stream) {
		// reset data in case of updates to existing files
		existingFile.setPageCount(0);
		existingFile.setDuration(0);
		existingFile.setWidth(0);
		existingFile.setHeight(0);
		existingFile.setVersion(null);
		existingFile.setDescription(null);
		existingFile.setParameters(null);
		existingFile.setScreenshotPath(null);
		existingFile.setErrorState(null);
		existingFile = FileDAO.save(existingFile);

		switch (existingFile.getFileType()) {
		case "pdf":
			try (PDDocument doc = PDDocument.load(stream)) {
				existingFile.setPageCount(doc.getNumberOfPages());
			} catch (InvalidPasswordException e) {
				logger.error("Invalid password.", e);
				return false;
			} catch (IOException e) {
				logger.error("IO exception.", e);
				return false;
			}
			break;

		case "jpg":
		case "png":
		case "gif":
		case "ico":
		case "svg":
		case "bmp":
			try {
				BufferedImage bimg = ImageIO.read(stream);
				existingFile.setWidth(bimg.getWidth());
				existingFile.setHeight(bimg.getHeight());
			} catch (IOException e) {
				logger.error("IO exception.", e);
				return false;
			}
			break;

		case "mp4":
			try (FFmpegFrameGrabber g = new FFmpegFrameGrabber(stream);
					ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				g.start();

				existingFile.setDuration(Math.toIntExact(g.getLengthInTime() / 1000000));
				existingFile.setWidth(g.getImageWidth());
				existingFile.setHeight(g.getImageHeight());

				g.stop();
			} catch (IOException e) {
				logger.error("IO exception.", e);
			}
			break;

		case "pptx":
			// convert to pdf
			XMLSlideShow ppt;
			try {
				ppt = new XMLSlideShow(stream);
			} catch (IOException e) {
				logger.error("IO exception.", e);
				return false;
			}

			// getting the dimensions and size of the slide, scale to improve quality
			Dimension pgsize = ppt.getPageSize();
			float scale = 2;
			int width = (int) (pgsize.width * scale);
			int height = (int) (pgsize.height * scale);

			List<XSLFSlide> slide = ppt.getSlides();
			BufferedImage img = null;
			Document doc = new Document();

			try (ByteArrayOutputStream pdfBos = new ByteArrayOutputStream()) {
				PdfWriter.getInstance(doc, pdfBos);
				doc.open();
				for (int i = 0; i < slide.size(); i++) {
					// prepare
					img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					Graphics2D graphics = img.createGraphics();
					graphics.setPaint(Color.white);
					graphics.fill(new Rectangle2D.Float(0, 0, width, height));
					graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
							RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
							RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					graphics.scale(scale, scale);

					// render
					slide.get(i).draw(graphics);
					ByteArrayOutputStream pageBos = new ByteArrayOutputStream();
					ImageIO.write(img, "png", pageBos);
					ppt.write(pageBos);

					// convert
					Image image = Image.getInstance(pageBos.toByteArray());
					doc.setPageSize(new Rectangle(image.getScaledWidth(), image.getScaledHeight()));
					doc.newPage();
					image.setAbsolutePosition(0, 0);
					doc.add(image);
					pageBos.close();
				}
				doc.close();

				// save new pdf file, delete original
				String fileKey = FilenameUtils.getBaseName(existingFile.getFileKey()) + ".pdf";
				byte[] pdfBytes = pdfBos.toByteArray();
				getECM().deleteDocumentByName(existingFile.getFileKey(), true);
				getECM().uploadDocument(fileKey, pdfBytes, mimeTypes.get("pdf"));
				existingFile.setDateModified(new Date());
				existingFile.setSize(pdfBytes.length);
				existingFile.setFileKey(fileKey);
				existingFile.setFileType("pdf");

				return addContentMetadata(existingFile, new ByteArrayInputStream(pdfBytes));
			} catch (Exception e) {
				logger.error("Conversion exception.", e);
				return false;
			}

		case "template":
			parseTemplate(existingFile, stream);
			break;
		}
		existingFile = FileDAO.save(existingFile);

		return true;

	}

	private boolean parseTemplate(File existingFile, InputStream stream) {
		ByteArrayOutputStream zipContent;
		String descriptor;
		try {
			zipContent = getZipContent(stream, "template.json");
			if (zipContent == null) {
				existingFile.setErrorState("No template.json descriptor found in archive.");
				return false;
			}
			descriptor = new String(zipContent.toByteArray(), "UTF-8");
		} catch (Exception e) {
			logger.error("Could not parse template archive.", e);
			existingFile.setErrorState("Could not parse template archive.");
			return false;
		}

		// validate descriptor
		try {
			JSONObject rawSchema = new JSONObject(
					new JSONTokener(IOUtils.resourceToString("/templateSchema.json", StandardCharsets.UTF_8)));
			Schema schema = SchemaLoader.load(rawSchema);
			schema.validate(new JSONObject(descriptor));
		} catch (ValidationException e) {
			existingFile.setErrorState(
					"Invalid format of template.json descriptor: " + ArrayUtils.toString(e.getAllMessages()));
			return false;
		} catch (JSONException e) {
			existingFile.setErrorState("JSON exception: " + e.getMessage());
			return false;
		} catch (IOException e) {
			logger.error("IO exception.", e);
			existingFile.setErrorState("Could not load template validation.");
			return false;
		}

		JsonObject params = new JsonParser().parse(descriptor).getAsJsonObject();
		String templateName = params.get("name").getAsString();
		String templateDescription = params.get("description").getAsString();
		String templateVersion = params.get("version").getAsString();
		String templateScreenshot = params.get("screenshot").getAsString();
		String templateParams = params.get("parameters").toString();

		existingFile.setScreenshotPath(templateScreenshot);
		existingFile.setName(templateName);
		existingFile.setDescription(templateDescription);
		existingFile.setVersion(templateVersion);
		existingFile.setParameters(templateParams);

		return true;
	}

	@DELETE
	@Path("/files/{fileId}")
	public Response deleteFile(@PathParam("fileId") long fileId) {
		File existingFile = getExistingFile(fileId, true);
		getECM().deleteDocumentByName(existingFile.getFileKey(), false);

		for (Owner owner : existingFile.getOwners()) {
			new OwnerDAO().deleteById(owner.getId());
		}

		return new FileDAO().deleteById(fileId) ? RESPONSE_OK : RESPONSE_BAD;
	}

	@GET
	@Path("/files/{fileId}/references")
	@Produces(MediaType.APPLICATION_JSON)
	public FileReferences getReferences(@PathParam("fileId") long fileId) {
		File existingFile = getExistingFile(fileId, true);
		FileReferences refs = new FileReferences();

		List<Page> pages = PageDAO.getPagesByFileOrWithTemplate(existingFile);
		for (Page page : pages) {
			if (page.getPageType() == PageType.TEMPLATE) {
				if (page.getFile() != null && new TemplateHelper(page.getFile().getParameters())
						.containsFile(existingFile, page.getTemplateValues())) {
					if (this.isAdmin() || inAdminOwners(page.getOwners())) {
						refs.addUserPage(page);
					} else {
						refs.setForeignPages(refs.getForeignPages() + 1);
					}
				}
			} else {
				if (this.isAdmin() || inAdminOwners(page.getOwners())) {
					refs.addUserPage(page);
				} else {
					refs.setForeignPages(refs.getForeignPages() + 1);
				}
			}
		}

		return refs;
	}

	@GET
	@Path("/files/{fileId}/content")
	@Produces({ "image/jpeg,image/png,image/gif,image/bmp,application/pdf,image/svg+xml,image/x-icon,video/mp4" })
	public Response getFileContent(@PathParam("fileId") long fileId,
			@DefaultValue("false") @QueryParam("thumbnail") boolean thumbnail,
			@DefaultValue("0") @QueryParam("page") int page,
			@DefaultValue("false") @QueryParam("download") boolean download) {

		File existingFile = getExistingFile(fileId, false);

		// template thumbnails come directly from the archive
		if (thumbnail && "template".equals(existingFile.getFileType()) && existingFile.getErrorState() == null) {
			return getFileContent(fileId, existingFile.getScreenshotPath(), 0);
		}

		String dateStr = new DateTime(existingFile.getDateModified()).toString(Consts.formatter);
		String eTag = existingFile.getId() + "_" + thumbnail + "_" + page + "_" + dateStr;

		// determine correct file to load
		String fileKey = existingFile.getFileKey()
				+ (thumbnail ? ".thumbnail" + (page > 0 ? "_" + page : "") + ".png" : "");
		String mimeType = mimeTypes.get(thumbnail ? "png" : existingFile.getFileType());
		if (page > 0 && !existingFile.getFileType().equals("pdf")) {
			page = 0;
		}

		final org.apache.chemistry.opencmis.client.api.Document doc;
		try {
			doc = getECM().getDocumentByName(fileKey);
		} catch (CmisObjectNotFoundException e) {
			if (thumbnail) {
				// create on demand
				if (createThumbnail(existingFile, page)) {
					return getFileContent(fileId, true, page, download);
				}
				return Response.ok(getClass().getResourceAsStream("/missing_screenshot.png"), mimeTypes.get("png"))
						.build();
			}
			logger.info("File not found: " + fileId);
			throwNotFound();
			return RESPONSE_NOTFOUND;
		}
		long length = doc.getContentStreamLength();

		// Check for range requests
		List<HTTPRange> ranges = new ArrayList<>();
		String range = request.getHeader("Range");
		if (range != null) {
			if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
				response.setHeader("Content-Range", "bytes */" + length);
				return Response.status(Status.REQUESTED_RANGE_NOT_SATISFIABLE).build();
			}

			if (ranges.isEmpty()) {
				for (String part : range.substring(6).split(",")) {
					long start = getLong(part, 0, part.indexOf("-"));
					long end = getLong(part, part.indexOf("-") + 1, part.length());

					if (start == -1) {
						start = length - end;
						end = length - 1;
					} else if (end == -1 || end > length - 1) {
						end = length - 1;
					}

					if (start > end) {
						response.setHeader("Content-Range", "bytes */" + length);
						return Response.status(Status.REQUESTED_RANGE_NOT_SATISFIABLE).build();
					}

					ranges.add(new HTTPRange(start, end, length));
				}
			}
		}

		response.setHeader("Accept-Ranges", "bytes");
		response.setHeader(HttpHeaders.ETAG, eTag);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename="
				+ existingFile.getName() + "." + existingFile.getFileType());

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream finalOut) throws IOException, WebApplicationException {
				if (ranges.isEmpty()) {
					streamDocument(doc, new HTTPRange(0, length - 1, length), finalOut, !ranges.isEmpty());
				} else if (ranges.size() == 1) {
					streamDocument(doc, ranges.get(0), finalOut, true);
				} else {
					response.setContentType("multipart/byteranges; boundary=MULTIPART_BYTERANGES");
					ServletOutputStream servletOut = (ServletOutputStream) finalOut;
					for (HTTPRange r : ranges) {
						servletOut.println();
						servletOut.println("--MULTIPART_BYTERANGES");
						servletOut.println("Content-Type: " + mimeType);

						streamDocument(doc, r, finalOut, true);
					}
				}
			}

			private void streamDocument(final org.apache.chemistry.opencmis.client.api.Document document,
					HTTPRange streamRange, OutputStream out, boolean setRangeHeader) throws IOException {
				if (setRangeHeader) {
					response.setHeader("Content-Range", "bytes " + streamRange.getStart() + "-" + streamRange.getEnd()
							+ "/" + streamRange.getTotal());
				}
				response.setHeader("Content-Length", String.valueOf(streamRange.getLength()));

				try (InputStream in = document.getContentStream(BigInteger.valueOf(streamRange.getStart()),
						BigInteger.valueOf(streamRange.getLength())).getStream()) {
					copyPartialStream(in, out, 0, streamRange.getLength());
				}
			}
		};

		return Response.status(ranges.isEmpty() ? Status.OK : Status.PARTIAL_CONTENT).type(mimeType)
				.lastModified(existingFile.getDateModified()).entity(stream).build();

	}

	private long getLong(String value, int beginIndex, int endIndex) {
		String substring = value.substring(beginIndex, endIndex);

		return (substring.length() > 0) ? Long.parseLong(substring) : -1;
	}

	private boolean createThumbnail(File file, int page) {
		switch (file.getFileType()) {
		case "pdf":
			try (PDDocument doc = PDDocument
					.load(getECM().getDocumentByName(file.getFileKey()).getContentStream().getStream());
					ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				PDFRenderer pdfRenderer = new PDFRenderer(doc);
				BufferedImage bimg = pdfRenderer.renderImageWithDPI(page > 0 ? (page - 1) : 0, 300, ImageType.RGB);
				BufferedImage resizedImage = resize(bimg, Consts.xDim, Consts.yDim);
				ImageIO.write(resizedImage, "png", bos);

				getECM().uploadDocument(file.getFileKey() + ".thumbnail" + (page > 0 ? "_" + page : "") + ".png",
						bos.toByteArray(), mimeTypes.get("png"));
				doc.close();

				return true;
			} catch (InvalidPasswordException e) {
				logger.error("Invalid password.", e);
			} catch (IOException e) {
				logger.error("IO exception.", e);
			}
			return false;

		case "jpg":
		case "png":
		case "gif":
		case "ico":
		case "svg":
		case "bmp":
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				BufferedImage bimg = ImageIO
						.read(getECM().getDocumentByName(file.getFileKey()).getContentStream().getStream());
				BufferedImage resizedImage = resize(bimg, Consts.xDim, Consts.yDim);
				ImageIO.write(resizedImage, "png", bos);

				getECM().uploadDocument(file.getFileKey() + ".thumbnail.png", bos.toByteArray(), mimeTypes.get("png"));

				return true;
			} catch (IOException e) {
				logger.error("IO exception.", e);
			}
			return false;

		case "mp4":
			try (FFmpegFrameGrabber g = new FFmpegFrameGrabber(
					getECM().getDocumentByName(file.getFileKey()).getContentStream().getStream());
					ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				Java2DFrameConverter converter = new Java2DFrameConverter();
				g.start();
				// TODO: will only work once streaming is activated, will then allow to take
				// time-dependent thumbnails
				// g.setTimestamp(30000);
				BufferedImage bimg = converter.getBufferedImage(g.grabImage());
				BufferedImage resizedImage = resize(bimg, Consts.xDim, Consts.yDim);
				g.stop();

				ImageIO.write(resizedImage, "png", bos);
				getECM().uploadDocument(file.getFileKey() + ".thumbnail.png", bos.toByteArray(), mimeTypes.get("png"));

				return true;
			} catch (IOException e) {
				logger.error("IO exception.", e);
			}
			return false;
		}
		return false;
	}

	@GET
	@Path("/files/{fileId}/content/{path}")
	@Produces({
			"text/html,image/jpeg,image/png,image/gif,image/bmp,application/pdf,image/svg+xml,image/x-icon,video/mp4" })
	public Response getFileContent(@PathParam("fileId") long fileId, @PathParam("path") String path,
			@DefaultValue("0") @QueryParam("page") long pageId) {
		File existingFile = getExistingFile(fileId, false);

		try (BufferedInputStream bis = new BufferedInputStream(
				getECM().getDocumentByName(existingFile.getFileKey()).getContentStream().getStream());
				ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			String fileExtension = FilenameUtils.getExtension(path);
			boolean doReplacement = "html".equalsIgnoreCase(fileExtension);

			ZipInputStream zin = new ZipInputStream(bis);
			ZipEntry ze = null;
			boolean found = false;
			while ((ze = zin.getNextEntry()) != null) {
				if (ze.getName().equals(path)) {
					if (!doReplacement) {
						response.setContentType(mimeTypes.get(fileExtension));
						response.setContentLength(Math.toIntExact(ze.getSize()));
					}

					byte[] buffer = new byte[Consts.BUFFER_SIZE];
					int len;
					while ((len = zin.read(buffer)) != -1) {
						if (doReplacement) {
							bos.write(buffer, 0, len);
						} else {
							response.getOutputStream().write(buffer, 0, len);
						}
					}
					found = true;
					break;
				}
			}

			if (!found) {
				throwNotFound();
			}

			if (doReplacement) {
				Page existingPage = new PageDAO().getById(pageId);
				if (existingPage == null) {
					throwNotFound();
				}

				// replace parameters
				String content = new String(bos.toByteArray(), "UTF-8");
				TemplateHelper helper = new TemplateHelper(
						existingPage.getFile() != null ? existingPage.getFile().getParameters() : null);
				content = helper.replace(content, existingPage.getTemplateValues());

				return Response.ok(content.getBytes("UTF-8"), mimeTypes.get(fileExtension)).build();
			} else {
				return RESPONSE_OK;
			}
		} catch (CmisObjectNotFoundException e) {
			logger.error("File not found.", e);
		} catch (IOException e) {
			logger.error("File could not be extracted.", e);
		}

		return RESPONSE_BAD;
	}

	@Override
	@POST
	@Path("/files/{fileId}/owners")
	@Consumes(MediaType.APPLICATION_JSON)
	public File addOwners(@PathParam("fileId") long fileId, Owner[] ownerData) {
		File existingFile = getExistingFile(fileId, true);
		doAddOwners(existingFile, ownerData);

		return FileDAO.save(existingFile);
	}

	@Override
	@DELETE
	@Path("/files/{fileId}/owners/{ownerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public File deleteOwner(@PathParam("fileId") long fileId, @PathParam("ownerId") long ownerId) {
		File existingFile = getExistingFile(fileId, true);
		doDeleteOwner(existingFile, ownerId);

		return FileDAO.save(existingFile);
	}

	@GET
	@Path("/screenkey/{key}")
	@Produces({ "image/jpeg,image/png,image/gif,image/bmp,application/pdf,image/svg+xml,image/x-icon,video/mp4" })
	public Response getScreenshotContent(@PathParam("key") String key) {
		String fileName = "int_screenshot_" + key;

		File existingFile = new FileDAO().getSingleByField("name", fileName);
		if (existingFile == null) {
			throwNotFound();
		}

		try {
			byte[] fileAsBytes = getECM().getDocumentAsByteArray(existingFile.getFileKey());
			String mimeType = mimeTypes.get(existingFile.getFileType());

			return Response.ok(fileAsBytes, mimeType).build();
		} catch (CmisObjectNotFoundException e) {
			logger.error("Screenshot could not be loaded.", e);
		}

		return RESPONSE_BAD;
	}

	@POST
	@Path("/screenkey/{key}")
	public Response uploadScreenshot(@PathParam("key") String key) {
		Screen existingScreen = ScreenDAO.getScreenByKey(key);
		if (existingScreen == null) {
			throwNotFound();
		}

		boolean uploadFile = false;

		// check if enough delay passed already, to not capture screens right while they
		// are loading
		if (existingScreen.isScreenshotMode() && existingScreen.getMetric_playlistTime() > 10) {
			long pageId = existingScreen.getMetric_currentPageId();
			Page existingPage = getExistingPage(pageId, false);

			if (existingPage != null) {
				uploadFile = true;

				File existingFile = doUploadScreenshot(key + "_" + pageId + "_auto", "int_screenshot_" + pageId + "_",
						Consts.xDim, Consts.yDim);

				existingPage.setAutoScreenshot(existingFile);
				PageDAO.save(existingPage);
			}
		}

		if (!uploadFile) {
			doUploadScreenshot(key, "int_screenshot_", 1280, 720);

			Date eventDate = new Date();
			existingScreen.setLastContainerAlive(eventDate);
			existingScreen = ScreenDAO.save(existingScreen);
			EventHistoryDAO.save(new EventHistory(existingScreen.getId(), eventDate));
		}

		return RESPONSE_OK;
	}

	@GET
	@Path("/ecm")
	@Produces(MediaType.APPLICATION_JSON)
	public List<File> getRawECM() {
		if (!isDBAdmin()) {
			throwUnauthorized();
		}

		List<CmisObject> docs = getECM().getFolderContent("/");
		List<File> result = new ArrayList<>();
		for (CmisObject doc : docs) {
			File file = new File(doc.getName());
			file.setFileKey(doc.getId());
			file.setSize(((BigInteger) doc.getPropertyValue("cmis:contentStreamLength")).longValue());
			file.setFileType(doc.getPropertyValue("cmis:contentStreamMimeType"));
			result.add(file);
		}

		return result;
	}

	@DELETE
	@Path("/ecm/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteECMFile(@PathParam("key") String ecmKey) {
		if (!isDBAdmin()) {
			throwUnauthorized();
		}

		getECM().deleteDocumentByKey(ecmKey, false);

		return RESPONSE_OK;
	}

	@POST
	@Path("/deletethumbnails")
	public Response deleteThumbnails() {
		if (!isAdmin()) {
			throwUnauthorized();
		}

		// file thumbnails are only saved to ECM without file objects
		List<CmisObject> docs = getECM().getFolderContent("/");
		for (CmisObject doc : docs) {
			String fileName = doc.getName().toLowerCase();
			if (fileName.contains("thumbnail")) {
				getECM().deleteDocumentByKey(doc.getId(), true);
			}
		}

		return RESPONSE_OK;
	}

	private void deleteThumbnails(String fileKey) {
		getECM().deleteDocumentByName(fileKey + ".thumbnail.png", true);

		// remove sub/page thumbnails as well
		int idx = 1;
		while (getECM().deleteDocumentByName(fileKey + ".thumbnail_" + idx + ".png", true)) {
			idx += 1;
		}
	}

	private void copyPartialStream(InputStream in, OutputStream out, long startIdx, long length) throws IOException {
		long skipped = in.skip(startIdx);

		if (skipped < startIdx) {
			throw new IOException("Skipped only " + skipped + " bytes out of requested " + startIdx);
		}

		byte buffer[] = new byte[Consts.BUFFER_SIZE];
		long bytesToCopy = length;

		int bytesRead;
		while ((bytesRead = in.read(buffer)) > 0) {
			if (bytesRead <= bytesToCopy) {
				out.write(buffer, 0, bytesRead);
				bytesToCopy -= bytesRead;
			} else {
				out.write(buffer, 0, (int) bytesToCopy);
				bytesToCopy = 0;
			}
			if (bytesToCopy == 0) {
				out.flush();
				break;
			}
		}
	}

}