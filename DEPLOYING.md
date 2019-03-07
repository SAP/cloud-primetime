# Deployment
## Building the application
A Maven build of the application generates one war file per environment and additional optional artifacts for different UI deployment scenarios. Due to the tight coupling to SAP Cloud Platform services this is currently the only deployment option available.

## Deploying to SAP Cloud Platform
SAP Cloud Platform supports multiple environments. See [this introduction](https://help.sap.com/viewer/65de2977205c403bbc107264b8eccf4b/Cloud/en-US/ab512c3fbda248ab82c1c545bde19c78.html) for more details. Depending on your criteria and existing resources you can deploy PrimeTime to either of them.
### Neo environment
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

### Cloud Foundry environment
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
