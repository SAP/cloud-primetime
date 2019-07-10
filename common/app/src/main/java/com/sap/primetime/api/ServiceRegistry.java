package com.sap.primetime.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.cxf.jaxrs.swagger.Swagger2Feature;

import com.sap.primetime.api.util.GsonMessageBodyHandler;


public class ServiceRegistry extends Application {
	private Set<Object> singletons = new HashSet<>();

	public ServiceRegistry() {
		singletons.add(new GsonMessageBodyHandler<>());

		singletons.add(new PlaylistService());
		singletons.add(new DBService());
		singletons.add(new FileService());
		singletons.add(new PageService());
		singletons.add(new ScreenService());
		singletons.add(new SystemService());
		singletons.add(new UserService());

		// Swagger API Documentation
		// More Swagger settings in swagger.properties
		Swagger2Feature feature = new Swagger2Feature();
		feature.setPrettyPrint(true);
		feature.setBasePath("/s/api");
		singletons.add(feature);
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}

}
