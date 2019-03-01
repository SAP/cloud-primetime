package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.primetime.dao.EventHistoryDAO;
import com.sap.primetime.dao.FileDAO;
import com.sap.primetime.dao.PageDAO;
import com.sap.primetime.dao.ScreenDAO;
import com.sap.primetime.dao.dto.FileReferences;
import com.sap.primetime.dao.entities.EventHistory;
import com.sap.primetime.dao.entities.File;
import com.sap.primetime.dao.entities.Owner;
import com.sap.primetime.dao.entities.Page;
import com.sap.primetime.dao.entities.Page.PageType;
import com.sap.primetime.dao.entities.Screen;
import com.sap.primetime.dao.entities.User;
import com.sap.primetime.testutil.HttpServletRequestMock;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.testutil.UploadHelper;

public class FileServiceTest extends PrimeTimeTest {
	private FileService fileService;
	private ScreenService screenService;
	private PageService pageService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		fileService = new FileService();
		fileService.request = requestMock;

		screenService = new ScreenService();
		screenService.request = requestMock;

		pageService = new PageService();
		pageService.request = requestMock;
	}

	@Test
	public void testGetFiles() throws Exception {
		assertEquals(0, getFiles().size());

		FileDAO.saveNew(new File("f2.jpg")); // without owner
		assertEquals(0, getFiles().size());

		fileService.createFile("f1", "jpg", "f1", 99);
		assertEquals(1, getFiles().size());

		fileService.createFile("f2", "jpg", "f2", 99);
		assertEquals(2, getFiles().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetFilesForOtherUser() throws Exception {
		fileService.createFile("f1", "jpg", "f1", 99);
		fileService.getFiles(simpleUser2.getUserId());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetNonExistingFile() throws Exception {
		fileService.getFile(-1);
	}

	@Test
	public void testGetFile() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);

		assertNotNull(fileService.getFile(file.getId()));
		assertEquals("f1", fileService.getFile(file.getId()).getName());
		assertEquals("jpg", fileService.getFile(file.getId()).getFileType());
	}

	@Test
	public void testDeleteFile() throws Exception {
		fileService.createFile("f1", "jpg", "f1", 99);
		assertEquals(1, getFiles().size());
		fileService.deleteFile(getFiles().get(0).getId());
		assertEquals(0, getFiles().size());
	}

	@Test
	public void testUploadFile() throws Exception {
		assertEquals(0, getFiles().size());
		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadFile();
		assertEquals(1, getFiles().size());
		File file = getFiles().get(0);
		assertEquals(simpleUser.getUserId(), file.getOwners().get(0).getUser().getUserId());
		assertEquals("png", file.getFileType());
		assertEquals(600, file.getWidth());
		assertEquals(400, file.getHeight());
		assertTrue(file.getName().endsWith("600x400"));
		assertTrue(file.getSize() > 0);
	}

	@Test
	public void testUploadPDFFile() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "test-3pages.pdf", "application/pdf");
		fileService.uploadFile();
		assertEquals(3, getFiles().get(0).getPageCount());

		UploadHelper.simulateFileUpload(requestMock, "test-4pages.pdf", "application/pdf");
		fileService.updateFileContent(getFiles().get(0).getId());
		assertEquals(4, getFiles().get(0).getPageCount());
	}

	@Test
	public void testUploadPPTXFile() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "test.pptx",
				"application/vnd.openxmlformats-officedocument.presentationml.presentation");
		fileService.uploadFile();
		assertEquals(2, getFiles().get(0).getPageCount());
		assertEquals("pdf", getFiles().get(0).getFileType());
	}

	@Test
	public void testUploadMP4File() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "small.mp4", "mp4");
		fileService.uploadFile();
		assertEquals(5, getFiles().get(0).getDuration());
		assertEquals(560, getFiles().get(0).getWidth());
		assertEquals(320, getFiles().get(0).getHeight());
	}

	@Test(expected = WebApplicationException.class)
	public void testUploadUnsupportedFile() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "textfile.txt", "text/plain");
		fileService.uploadFile();
	}

	@Test
	public void testUpdateFile() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadFile();
		assertEquals("png", getFiles().get(0).getFileType());

		UploadHelper.simulateFileUpload(requestMock, "500x300.jpg", "image/jpg");
		fileService.updateFileContent(getFiles().get(0).getId());
		assertEquals(1, getFiles().size());
		File file = getFiles().get(0);
		assertEquals(500, file.getWidth());
		assertEquals(300, file.getHeight());
		assertEquals("jpg", file.getFileType());
	}

	@Test(expected = WebApplicationException.class)
	public void testUploadScreenshotForNonExistingScreen() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("nonexisting");
	}

	@Test
	public void testUploadScreenshot() throws Exception {
		Screen screen = screenService.createScreen(new Screen("s1"));
		screen.setKey("k1");
		screen = ScreenDAO.save(screen);

		assertNull(screen.getLastContainerAlive());
		assertEquals(0, new EventHistoryDAO().getCount());

		UploadHelper.simulateFileUpload(requestMock, "600x400.png", "image/png");
		fileService.uploadScreenshot("k1");

		screen = screenService.getScreen(screen.getId(), true, false);
		assertNotNull(screen.getLastContainerAlive());
		assertEquals(1, new EventHistoryDAO().getCount());

		EventHistory event = new EventHistoryDAO().getAll().get(0);
		assertEquals(EventHistory.EventType.ScreenAlive, event.getEventType());
		assertEquals(screen.getId(), event.getEntityId());
		assertNotNull(event.getEventDate());
	}

	@Test
	public void testAddOwners() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		assertEquals(1, fileService.getFile(file.getId()).getOwners().size());

		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i1")) });
		assertEquals(2, fileService.getFile(file.getId()).getOwners().size());
		List<Owner> owners = fileService.getFile(file.getId()).getOwners();
		assertEquals("i1", owners.get(1).getUser().getUserId());

		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i2")) });
		assertEquals(3, fileService.getFile(file.getId()).getOwners().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddUnauthorizedOwner() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		fileService.request = requestMock;
		fileService.addOwners(file.getId(), new Owner[] { new Owner(simpleUser2) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("wrongUserId")) });
	}

	@Test(expected = WebApplicationException.class)
	public void testAddInvalidOwner2() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i1i2")) });
	}

	@Test
	public void testNoOwner() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		assertEquals(1, getFiles().size());

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		fileService.request = requestMock;
		assertEquals(0, getFiles().size());

		// switch user to add owner
		requestMock = new HttpServletRequestMock(simpleUser);
		fileService.request = requestMock;
		fileService.addOwners(file.getId(), new Owner[] { new Owner(simpleUser2) });

		// switch back
		requestMock = new HttpServletRequestMock(simpleUser2);
		fileService.request = requestMock;
		assertEquals(1, getFiles().size());
	}

	@Test(expected = WebApplicationException.class)
	public void testAddDuplicateOwner() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i1")) });
		fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i1")) });
	}

	@Test
	public void testDeleteOwner() throws Exception {
		File file = fileService.createFile("f1", "jpg", "f1", 99);
		file = fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i1")) });
		file = fileService.addOwners(file.getId(), new Owner[] { new Owner(new User("i2")) });

		assertEquals(3, fileService.getFile(file.getId()).getOwners().size());
		fileService.deleteOwner(file.getId(), file.getOwners().get(1).getId());

		assertEquals(2, fileService.getFile(file.getId()).getOwners().size());
		assertEquals("i2", fileService.getFile(file.getId()).getOwners().get(1).getUser().getUserId());
	}

	@Test
	public void testExtractFileName() throws Exception {
		assertEquals("file.extension", FilenameUtils.getName("file.extension"));
		assertEquals("file.extension", FilenameUtils.getName("c:\\test\\file.extension"));
		assertEquals("file.extension", FilenameUtils.getName("c:/test/file.extension"));
	}

	@Test
	public void testGetReferences() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "testtemplate.zip", "application/zip");
		fileService.uploadFile();
		File template = fileService.getFiles(null).get(0);

		File file1 = fileService.createFile("f1", "jpg", "f1", 99);

		Page page1 = new Page("p1");
		page1.setFile(file1);
		page1 = pageService.createPage(page1);

		Page page2 = new Page("p2");
		page2.setFile(file1);
		page2 = pageService.createPage(page2);

		// one page where user is not owner
		page2.getOwners().clear();
		page2 = PageDAO.save(page2);

		FileReferences refs = fileService.getReferences(file1.getId());
		assertEquals(1, refs.getUserPages().size());
		assertEquals(1, refs.getForeignPages());

		// used in template
		Page page3 = new Page("p3");
		page3.setPageType(PageType.TEMPLATE);
		page3.setFile(template);
		page3.setTemplateValues("{\"image\":\"" + file1.getId() + "\"}");
		page3 = pageService.createPage(page3);

		refs = fileService.getReferences(file1.getId());
		assertEquals(2, refs.getUserPages().size());
		assertEquals(1, refs.getForeignPages());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesForNonExistingPage() throws Exception {
		fileService.getReferences(-1);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetReferencesAsUnauthorized() throws Exception {
		File file1 = fileService.createFile("f1", "jpg", "f1", 99);

		Page page1 = new Page("p1");
		page1.setFile(file1);
		page1 = pageService.createPage(page1);

		// switch user
		requestMock = new HttpServletRequestMock(simpleUser2);
		pageService.request = requestMock;

		pageService.getReferences(file1.getId());
	}

	@Test
	public void testCreateTemplate() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "testtemplate.zip", "application/zip");
		fileService.uploadFile();

		File file = getFiles().get(0);
		assertEquals(1, file.getOwners().size());
		assertEquals("Test template", file.getName());
		assertEquals("Test description", file.getDescription());
		assertEquals("1.0", file.getVersion());
		assertEquals("preview.gif", file.getScreenshotPath());
		assertNull(file.getErrorState());
		assertNotNull(file.getParameters());
	}

	@Test
	@Ignore("File serving is not testable yet")
	public void testGetTemplateContent() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "testtemplate.zip", "application/zip");
		fileService.uploadFile();
		File file1 = getFiles().get(0);

		Page page1 = new Page("p1");
		page1.setPageType(PageType.TEMPLATE);
		page1.setFile(file1);
		page1 = pageService.createPage(page1);

		assertNotNull(fileService.getFileContent(file1.getId(), "index.html", page1.getId()));

		// simulate special draft case
		file1.setParameters(null);
		file1 = FileDAO.save(file1);

		assertNotNull(fileService.getFileContent(file1.getId(), "index.html", page1.getId()));
	}

	@Test
	public void testUploadInvalidTemplate() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "testtemplate3.zip", "application/zip");
		fileService.uploadFile();

		File file = getFiles().get(0);
		assertEquals(1, file.getOwners().size());
		assertNotNull(file.getErrorState());
		assertEquals("testtemplate3", file.getName());
		assertEquals("template", file.getFileType());
		assertNull(file.getDescription());
		assertNull(file.getVersion());
		assertNull(file.getScreenshotPath());
	}

	@Test
	public void testUpdateTemplate() throws Exception {
		UploadHelper.simulateFileUpload(requestMock, "testtemplate.zip", "application/zip");
		fileService.uploadFile();

		UploadHelper.simulateFileUpload(requestMock, "testtemplate2.zip", "application/zip");
		fileService.updateFileContent(getFiles().get(0).getId());

		List<File> files = getFiles();
		assertEquals(1, files.size());
		assertEquals(1, files.get(0).getOwners().size());
		assertEquals("Test template 2", files.get(0).getName());
		assertEquals("Test description 2", files.get(0).getDescription());
		assertEquals("2.0", files.get(0).getVersion());
		assertEquals("preview.gif", files.get(0).getScreenshotPath());
		assertNull(files.get(0).getErrorState());
		assertNotNull(files.get(0).getParameters());
	}

	private List<File> getFiles() {
		return fileService.getFiles(null);
	}
}
