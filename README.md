PDex Payer Data Exchange Reference Implementation
===============

Da Vinci reference implementation for the Payer Data Exchange use case. More details about use case (Scenario 1) here: https://confluence.hl7.org/display/DVP/PDex+Reference+Implementation

# Sources
This is a Maven multi-module project with two Spring-Boot Applications: Provider Smart App and Payer CDS-Hooks Service.
### Build
```sh
mvn clean install
```
### Deploy
All Apps are automatically deployed to [Heroku](https://dashboard.heroku.com):
* Payer CDS-Hooks Services: https://payer-cds-hooks-service.herokuapp.com/cds-services
* Provider (Can be launched only through HSPC EHR): https://provider-smart-app.herokuapp.com

Sources are built from [this GitHub repository](https://github.com/HL7-DaVinci/PDex-Patient-Import-App) and redeployed automatically on every new commit.

### Docker
Both apps can be easilly deployed in a Docker container.
Provider application configuratons and run:
```sh
export PAYER_FHIR_SERVER_URI=...
export PATIENT_DATA_IMPORT_SMART_APP_URI=...
export PROVIDER_FHIR_SERVER_URI=...
export PAYER_CDS_HOOK_URI=...
export SECURITY_OAUTH2_CLIENT_CLIENT_ID=..
docker-compose up --build
```
`--detach` is optional
To change application properties (e.g. Fhir Server URL in a Payer app) stop Docker container, set the corresponding configuration property, and start it back:
```sh
docker-compose stop payer-cds-hooks-service
export PAYER_FHIR_SERVER_URI=another value
docker-compose up  --detach payer-cds-hooks-service
```
