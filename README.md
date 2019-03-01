# Introduction
PrimeTime is a free digital signage solution originating from SAP. It helps you to easily manage contents for screens of all types - supporting many different formats, custom templates as well as public and secured content. It also acts as a technical showcase of how SAP Cloud Platform can be utilized.

![PrimeTime](https://raw.githubusercontent.com/SAP/cloud-primetime/master/common/web/src/main/webapp/ui/img/logo.png)

# Try It

Check out our [demo system](https://primetime.eu2.hana.ondemand.com) and [view an example](https://primetime.eu2.hana.ondemand.com/?screen=0) of some of the things possible!

# Description
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

# Requirements
- [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (version 1.8.0_131 or higher)
- [Maven](https://maven.apache.org/download.cgi) (version 3.3.9 or higher)
- [Neo Environment SDK](https://tools.hana.ondemand.com/#cloud) (version 3.68.11 or higher) or [CF CLI](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/4ef907afb1254e8286882a2bdef0edf4.html) (version 6.23.1 or higher)
- If you do not yet have an SAP Cloud Platform trial or enterprise account, sign up for one by following the [documentation](https://cloudplatform.sap.com/try.html).

# Download and installation

## Running the application locally
  - Check out and import the project as new Maven project into Eclipse
  - Install the SAP Cloud Platform [Tools for Eclipse](https://tools.hana.ondemand.com/#cloud)
  - Download and extract (to a folder of your choice) the SAP Cloud Platform [Neo Environment SDK](https://tools.hana.ondemand.com/#cloud)
  - Create a new local "SAP/Java Web Tomcat 8 Server" in Eclipse, pointing to the downloaded Neo SDK
  - Add `web-neo` to the server
  - Set the module root to "/" (double-click on server, then go to modules)
  - Create a user with roles "admin" and "dbadmin" in the server (any Id, name, mail, and password)
  - Optional: if you want to use the file upload feature, install [MongoDB community edition](https://help.sap.com/viewer/b0cc1109d03c4dc299c215871eed8c42/Cloud/en-US/1c6d4a951e7c48c1acfd29b63b56ef43.html)
  - Optional: create a destination "featureflags" and point it to valid [Cloud Foundry feature flags service](https://help.sap.com/viewer/2250efa12769480299a1acd282b615cf/Cloud/en-US/29788680118a41cb85b6bb691507f821.html) key
  - Optional: click "Create Sample Data" in the app header after the app [is started](http://localhost:8080) to get sample data created

## Building the application
A Maven build of the application generates one war file per environment and additional optional artifacts for different UI deployment scenarios.

## Deploying to the Neo environment
Supported scenarios:
1. Java and UI deployed as one app, UI served from backend (easiest)
2. UI deployed to HTML5 runtime using a destination to forward to separately deployed Java backend

Independent of the scenario:

  - At first time only:
    - Copy the provided `deployTemplate.properties` from the `neo` directory to a new file, e.g. `deployTest.properties` (these files will automatically be ignored from git and not committed)
    - Replace the placeholders in there with your personal settings (account & user, if not using the trial region also host & platform-domain)
    - Run `neo deploy deployTest.properties` from the `neo` directory
    - Create an ASE or HANA database schema in the [Cloud Cockpit](https://account.hanatrial.ondemand.com/cockpit) and bind it to the application
    - Create a platform domain for the CERT authentication option needed by the Apple TV application by executing `neo add-platform-domain deployTest.properties`
    - Optional: create a destination 'featureflags' and point it to a valid [Cloud Foundry feature flags service](https://help.sap.com/viewer/2250efa12769480299a1acd282b615cf/Cloud/en-US/29788680118a41cb85b6bb691507f821.html) key
  - Run `neo rolling-update deployTest.properties` for zero-downtime deployment (or alternatively `neo deploy deployTest.properties` and `neo restart deployTest.properties` if you have less resources available)
  - At first time only:
    - Go to Security in the application details or your deployed application in the [Cloud Cockpit](https://account.hanatrial.ondemand.com/cockpit) and add yourself to the "admin" and "dbadmin" roles

Additional steps for scenario 2:

  - Subscribe the Java application to a new account (AppToAppSSO only works if both URLs are exposed through tenant URLs)
  - Create a new HTML5 app, import the `neo/html5-neo/target/html5.zip` file and activate the version
  - Create a new destination named "primetime", pointing to the subscribed Java application, with authentication AppToAppSSO

## Deploying to the Cloud Foundry environment
Supported scenarios:
1. Java and UI deployed as one app, with an AppRouter simply forwarding all requests
2. AppRouter serving static UI directly and forwarding all other calls to Java backend (recommended)
3. AppRouter forwarding calls to UI to HTML5 repository service and all other calls to Java backend (some parts under construction)

Independent of the scenario:
  - At first time only:
    - Copy the provided `varsTemplate.yml` from the `cf` directory to a new file, e.g. `varsTest.yml` (these files will automatically be ignored from git and not committed), set the `appName` in there to a unique value, e.g. by adding your user Id at the end, and the host to whatever region you are using
    - Run `npm config set @sap:registry https://npm.sap.com`
    - Run `cf create-service postgresql v9.6-dev primetime-postgres`
    - Run `cf create-service xsuaa application primetime-uaa -c xs-security.json`
    - Run `cf create-service html5-apps-repo app-host primetime-html5-host`
    - Run `cf create-service html5-apps-repo app-runtime primetime-html5-rt`
  - Run `mvn clean install` in the root to have all node dependencies downloaded
  - Run `cf push --vars-file varsTest.yml` from the `cf` directory
  - At first time only:
    - Create a new Role Collection, e.g. "Super Admin", on subaccount level in the [Cloud Cockpit](https://account.hanatrial.ondemand.com/cockpit), and add the roles "admin" and "dbadmin" to it
    - Go to the Trust Configuration "SAP ID Service" and assign the new role collection to your user
  
# Configuration
There are multiple configuration options available which are applied in a pre-defined order. If a property value is found in a later stage, it will overwrite the previous ones.

1. base properties file (shipped with the repository)
2. custom properties file (created by you)
2. system variable
3. environment variable (set at deploy time)
4. database (set at runtime) 

The easiest way to configure PrimeTime is at runtime through the "Configuration" tab, which requires the "admin" role. Any changes you make there will be persisted in the database.

# Limitations
The document service is not yet available in the Cloud Foundry environment and file support is deactivated when deploying there.

# Known Issues
None

# How to obtain support
Please use GitHub [issues](https://github.com/SAP/cloud-primetime/issues/new) for any bugs to be reported.

# Contributing
Contributions are very welcome.

# To-Do (upcoming changes)
  - Add MTA support 
  - Switch to WebSockets for polling scenarios
  - Provide K8s as a deployment option
  - Evaluate switching to TypeScript 

# License
Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved. This file is licensed under SAP Sample Code License Agreement, except as noted otherwise in the [LICENSE](/LICENSE) file.