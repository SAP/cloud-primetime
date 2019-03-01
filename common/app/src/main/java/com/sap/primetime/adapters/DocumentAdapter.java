package com.sap.primetime.adapters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class DocumentAdapter {
	private static Logger logger = LoggerFactory.getLogger(DocumentAdapter.class);
	private Session cmisSession = null;

	@Inject
	CMISProvider cmisProvider;

	public Document uploadDocument(String documentName, byte[] documentContent, String mimeType)
			throws CmisNameConstraintViolationException {
		if (documentExists(documentName)) {
			deleteDocumentByName(documentName, false);
		}

		return createDocument(documentName, documentContent, mimeType);
	}

	private boolean documentExists(String documentName) {
		try {
			getDocumentByName(documentName);
		} catch (CmisObjectNotFoundException e) {
			return false;
		}

		return true;
	}

	public Document getDocumentByName(String documentName) {
		Document document = null;
		Session session = getCmisSession();
		if (session == null) {
			logger.error("ECM not found, session is null.");
			return null;
		}
		try {
			document = (Document) session.getObjectByPath("/" + documentName);
		} catch (ClassCastException e) {
			logger.error("The path does not point to a document.", e);
		}

		return document;
	}

	public Document getDocumentByKey(String documentKey) {
		Document document = null;
		Session session = getCmisSession();
		if (session == null) {
			logger.error("ECM not found, session is null.");
			return null;
		}
		try {
			document = (Document) session.getObject(documentKey);
		} catch (ClassCastException e) {
			logger.error("The Id does not point to a document.", e);
		}

		return document;
	}

	public Session getCmisSession() {
		if (cmisSession == null) {
			cmisSession = cmisProvider.getCmisSession();
		}
		return cmisSession;
	}

	private Document createDocument(String documentName, byte[] documentContent, String mimeType)
			throws CmisNameConstraintViolationException {
		Session session = getCmisSession();
		if (session == null) {
			logger.error("ECM not found, session is null.");
			return null;
		}

		Folder root = session.getRootFolder();

		Map<String, Object> properties = getProperties(documentName);
		ContentStream contentStream = getContentStream(documentName, mimeType, documentContent);

		return root.createDocument(properties, contentStream, VersioningState.NONE);
	}

	private Map<String, Object> getProperties(String documentName) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		properties.put(PropertyIds.NAME, documentName);

		return properties;
	}

	private ContentStream getContentStream(String documentName, String mimeType, byte[] documentContent) {
		InputStream stream = new ByteArrayInputStream(documentContent);
		ContentStream contentStream = getCmisSession().getObjectFactory().createContentStream(documentName,
				documentContent.length, mimeType, stream);

		return contentStream;
	}

	public byte[] getDocumentAsByteArray(String documentName) throws CmisObjectNotFoundException {
		Document document = getDocumentByName(documentName);
		byte[] documentAsByteArray = convertDocumentToByteArray(document);

		return documentAsByteArray;
	}

	private byte[] convertDocumentToByteArray(Document document) {
		byte[] documentAsBytes = null;
		try (InputStream stream = document.getContentStream().getStream()) {
			documentAsBytes = IOUtils.toByteArray(stream);
		} catch (IOException e) {
			logger.error("Could not convert document to byte array.", e);
		}

		return documentAsBytes;
	}

	public boolean deleteDocumentByName(String documentName, boolean silent) {
		try {
			Document documentToBeDeleted = getDocumentByName(documentName);
			if (documentToBeDeleted != null) {
				documentToBeDeleted.deleteAllVersions();
				return true;
			}
		} catch (CmisObjectNotFoundException e) {
			if (!silent) {
				logger.info("Document '" + documentName + "' does not exist in repository.", e);
			}
		}
		return false;
	}

	public boolean deleteDocumentByKey(String documentKey, boolean silent) {
		try {
			Document documentToBeDeleted = getDocumentByKey(documentKey);
			if (documentToBeDeleted != null) {
				documentToBeDeleted.deleteAllVersions();
				return true;
			}
		} catch (CmisObjectNotFoundException e) {
			if (!silent) {
				logger.info("Document wih Id '" + documentKey + "' does not exist in repository.", e);
			}
		}
		return false;
	}

	public List<CmisObject> getFolderContent(String path) {
		Session cmiSession = getCmisSession();
		if (cmiSession != null) {
			CmisObject folder = cmiSession.getObjectByPath(path);
			List<CmisObject> items = listFolder((Folder) folder);
			return items;
		}
		return new ArrayList<>();
	}

	private List<CmisObject> listFolder(Folder folder) {
		List<CmisObject> items = new ArrayList<>();
		try {
			ItemIterable<CmisObject> itemIterable = folder.getChildren();
			Iterator<CmisObject> iterator = itemIterable.iterator();
			while (iterator.hasNext()) {
				CmisObject object = iterator.next();
				items.add(object);
			}
		} catch (Exception e) {
			logger.error("Could not retrieve folder listing.", e);
		}
		return items;
	}

}
