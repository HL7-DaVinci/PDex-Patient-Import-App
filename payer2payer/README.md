
# Implementation Details
Implementation follows the use case described in the documentation to prove the following functionality:
1. OAuth2 Authorization for a Payer B Smart App
2. OAuth2 Authorization for a Payer A FHIR Server
3. Querying of Patient data from a Payer A FHIR Server by a Payer B Smart App using a token requested during step 2
4. Persistance of Patient data via a Capability Statement
5. Storing unsupported record data as a DocumentReference

# Sources
### Deploy
All Apps are automatically deployed to [Heroku](https://dashboard.heroku.com):
* Payer B Smart App (Can be launched only through HSPC EHR): https://payer-b-smart-app.herokuapp.com

### Docker
//TODO

# Try It
* **Run a Scenario**:
Go to DaVinciPDexPayer Sandbox at https://sandbox.hspconsortium.org/DaVinciPDexPayer and launch an app RI-S2-Payer-Smart-App in the Apps section.
