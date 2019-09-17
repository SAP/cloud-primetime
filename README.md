# PrimeTime
## Introduction
PrimeTime is a digital signage solution. It helps you to easily manage contents for screens of all types - supporting many different formats, custom templates as well as public and secured content where a prior login is required. It also acts as a technical showcase of how SAP Cloud Platform can be utilized.

![PrimeTime](/common/web/src/main/webapp/ui/img/logo.png)

## Try It

Check out our [demo system](https://primetime.eu2.hana.ondemand.com) and [view an example](https://primetime.eu2.hana.ondemand.com/?screen=0) of some of the things possible! In case you have no user to login yet, you can register for free on [SAP.com](https://www.sap.com).

## Description
Easy to get started
  - Create pages representing your contents
  - Add these pages to a playlist
  - Display this playlist on your screen
  - All managed through the cloud, without any mandatory client installation needed
  
Easy online administration
  - Manage multiple playlists with any number of pages
  - Use multiple page types: single and multiple URLs, PDFs, custom text including basic markup, MP4 movies, YouTube, SAP MediaShare, freestyle templates 
  - Supports uploading your own files (PDFs, images, movies, templates...) 
  - Supports any number of screens, with optional automatic onboarding through an Apple TV app (coming soon)
  - Lots of additional features: page owners, page reordering, public page catalog, automatic cover image creation, custom rotation duration, footer customization, auto-reload upon changes... 

## Requirements
### Build & Deploy requirements
- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (version 1.8.0_131 or higher)
- [Maven](https://maven.apache.org/download.cgi) (version 3.3.9 or higher)
- [Neo Environment SDK](https://tools.hana.ondemand.com/#cloud) (version 3.68.11 or higher) or [CF CLI](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/4ef907afb1254e8286882a2bdef0edf4.html) (version 6.23.1 or higher)
- If you do not yet have an SAP Cloud Platform trial or enterprise account, sign up for one by following the [documentation](https://cloudplatform.sap.com/try.html).
### Additional development requirements
- [Eclipse Java EE IDE](https://www.eclipse.org/downloads/packages/) (version 2018-12 or higher)
-  SAP Cloud Platform [Tools for Eclipse](https://tools.hana.ondemand.com/#cloud) (version 3.68.11 or higher)
  - Optional: if you want to use the file upload feature, install [MongoDB community edition](https://help.sap.com/viewer/b0cc1109d03c4dc299c215871eed8c42/Cloud/en-US/1c6d4a951e7c48c1acfd29b63b56ef43.html)
  
## Download and installation

### Running the application locally
  - Check out and import the project as new Maven project into Eclipse
  - Create a new local "SAP/Java Web Tomcat 8 Server" in Eclipse, pointing to the downloaded Neo SDK (for additional help use [this tutorial](https://developers.sap.com/tutorials/hcp-java-eclipse-setup.html))
  - Add the `web-neo` module to the server you just created
  - Set the module root to "/" (double-click on server, then go to modules)
  - Create a user with roles "admin" and "dbadmin" in the server with any Id, name, mail, and password (double-click on server, then go to users)
  - Optional: create a destination "featureflags" and point it to valid [Cloud Foundry feature flags service](https://help.sap.com/viewer/2250efa12769480299a1acd282b615cf/Cloud/en-US/29788680118a41cb85b6bb691507f821.html) key (double-click on server, then go to connectivity)
  - Optional: click "Create Sample Data" in the app header after the app [is started](http://localhost:8080) to get sample data created

### Building & Deploying the application
A Maven build of the application generates one war file per environment and additional optional artifacts for different UI deployment scenarios. These are described in a [separate document](/DEPLOYING.md).

## Configuration
There are multiple configuration options available which are applied in a pre-defined order. If a property value is found in a later stage, it will overwrite the previous ones.

1. base properties file (shipped with the repository, see app.properties)
2. custom properties file (created by you, e.g. app.2.properties)
3. system variable
4. environment variable (set at deploy time)
5. database (set at runtime) 

The easiest way to configure PrimeTime is at runtime through the "Configuration" tab, which requires the "admin" role. Any changes you make there will be persisted in the database.

There is also an API available which is described through a [Swagger UI](https://primetime.eu2.hana.ondemand.com/s/api/api-docs?url=/s/api/swagger.json#/default).

## Limitations
  - The [SapMachine OpenJDK](https://sap.github.io/SapMachine/) is currently not supported. There will be an error in one unit test and the server adapter will not start.
  - Deployment to cloud platforms other than SAP Cloud Platform is not supported.
  - The document service is not yet available in the Cloud Foundry environment and file support is deactivated when deploying there.

## Known Issues
None

## How to obtain support
Please use GitHub [issues](https://github.com/SAP/cloud-primetime/issues/new) for any bugs to be reported.

## Contributing
Contributions are very welcome.

## To-Do (upcoming changes)
  - Add MTA support 
  - Switch to WebSockets for polling scenarios
  - Provide K8s as a deployment option
  - Evaluate switching to TypeScript 

## License
Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved. This file is licensed under the Apache Software License v2, except as noted otherwise in the [LICENSE](/LICENSE) file.