package com.sap.primetime.adapters;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.ecm.api.EcmService;
import com.sap.ecm.api.RepositoryOptions;
import com.sap.primetime.adapters.CMISProvider;

public class NeoCMISProvider implements CMISProvider {
	private static Logger logger = LoggerFactory.getLogger(NeoCMISProvider.class);
	// FIXME: externalize
	private static final String UNIQUE_NAME = "com.sap.tgif.mercury.ecm.document.repository.01";
	private static final String UNIQUE_KEY = "com.sap.tgif.mercury.ecm.h14PJthdmpskFFjlk";

	@Override
	public Session getCmisSession() {
		Session session = null;
		try {
			InitialContext ctx = new InitialContext();
			String lookupName = "java:comp/env/EcmService";
			EcmService ecmSvc = (EcmService) ctx.lookup(lookupName);
			try {
				session = ecmSvc.connect(UNIQUE_NAME, UNIQUE_KEY);
				logger.info("Connection to ECM repository established.");
			} catch (CmisObjectNotFoundException e) {
				RepositoryOptions options = new RepositoryOptions();
				options.setUniqueName(UNIQUE_NAME);
				options.setRepositoryKey(UNIQUE_KEY);
				options.setVisibility(com.sap.ecm.api.RepositoryOptions.Visibility.PROTECTED);
				options.setMultiTenantCapable(true);
				ecmSvc.createRepository(options);

				session = ecmSvc.connect(UNIQUE_NAME, UNIQUE_KEY);
			}
		} catch (NamingException e) {
			logger.error("Could not find the ECM service.", e);
		}
		return session;
	}

}
