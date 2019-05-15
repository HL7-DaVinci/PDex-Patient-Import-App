PDex Payer Data Exchange Reference Implementation
===============

Da Vinci reference implementation for the Payer Data Exchange use case. More details about use case (Scenario 1) here: https://confluence.hl7.org/display/DVP/PDex+Reference+Implementation

# Implementation Details
Implementation follows the use case described in the documentation to prove the following functionality:
1. OAuth2 Authorization for a Provider Smart App.
2. Delegation of an OAuth token to a CDS-Hooks service via a CDS-Hook request.
3. Patient Matching
4. Querying of Patient data from a Payer system
5. Import Data selection
6. Persistance of Patient data via a Capability Statement
7. Storing unsupported record data as a DocumentReference

Some functionality is out of scope in this implementation but might be implemented in the nearest future:
1. Provider App needs a FHIR Server URL and Application ID at a start time. We are using spring-security-oauth2-autoconfigure plugin and did not focus on extending the default functionality much, since that was enough for a proof of concept.
2. Provider and Payer FHIR Servers follow the R4 spec only.
3. CDS-Hooks Service can connect to a Payer FHIR Server only through an open endpoint.

# Sources
This is a Gradle multi-module project with two Spring-Boot Applications: Provider Smart App and Payer CDS-Hooks Service.
### Build
```sh
gradlew clean build
```
### Deploy
All Apps are automatically deployed to [Heroku](https://dashboard.heroku.com):
* Payer CDS-Hooks Services: https://payer-cds-hooks-service.herokuapp.com/cds-services
* Provider (Can be launched only through HSPC EHR): https://provider-smart-app.herokuapp.com

Sources are built from [this GitHub repository](https://github.com/HL7-DaVinci/PDex-Patient-Import-App) and redeployed automatically on every new commit.

### Docker
Both apps can be easily deployed in a Docker container.
Provide Payer CDS-Hooks Service configuration:
```sh
export PAYER_FHIR-SERVER-URI: ...
export PATIENT-DATA-IMPORT_SMART-APP-URI: ...
```
* **PAYER_FHIR-SERVER-URI**: FHIR server open endpoint for payer data
* **PATIENT-DATA-IMPORT_SMART-APP-URI**: Link to Provider Smart App for Data Import returned in a CDS Hook Card

Provide Provider Smart App configuration:
```sh
export PROVIDER_FHIR_SERVER_URI=...
export PAYER_CDS_HOOK_URI=...
export SECURITY_OAUTH2_CLIENT_CLIENT_ID=..
```
* **PROVIDER_FHIR_SERVER_URI**: FHIR server secured endpoint for Patient data
* **PATIENT-PAYER_CDS_HOOK_URI**: CDS Hook Service URI
* **PATIENT-SECURITY_OAUTH2_CLIENT_CLIENT_ID**: App client ID from HSPC Sandbox

To start run the following command:
```sh
docker-compose up --build
```
`--detach` is optional
To change application properties (e.g. Fhir Server URL in a Payer app) stop Docker container, set the corresponding configuration property, and start it back:
```sh
docker-compose stop payer-cds-hooks-service
export PAYER_FHIR_SERVER_URI=another value
docker-compose up  --detach payer-cds-hooks-service
```
# Try it
* **Run a Scenario**:
Go to DaVinciPDexProvider Sandbox at https://sandbox.hspconsortium.org/DaVinciPDexProvider and launch an app RI-Provider-Smart-App in the Apps section.
* **Play with CDS Hook services** at: https://payer-cds-hooks-service.herokuapp.com/cds-services. Currently a single service is available: **smart-appointment-hook**
