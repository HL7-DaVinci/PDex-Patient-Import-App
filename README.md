PDex Payer Data Exchange Reference Implementation
===============

Da Vinci reference implementations for the Payer Data Exchange use case. More details about use case (Scenario 1 and 2) here: https://confluence.hl7.org/display/DVP/PDex+Reference+Implementation

# Sources
This is a Gradle multi-module project with a module per scenario.
1. [payer2provider](payer2provider) - Implementation of Scenario 1.
2. [payer2payer](payer2payer) - Implementation of Scenario 2.

Refer to module README for more details.

### Build
```sh
gradlew clean build
```
### Deploy
All Apps are automatically deployed to [Heroku](https://dashboard.heroku.com). Refer to module README for more details.

Sources are built from [this GitHub repository](https://github.com/HL7-DaVinci/PDex-Patient-Import-App) and redeployed automatically on every new commit to master.

### Docker
All apps can be deployed as Docker images. Refer to module README for more details.

# Try it
Refer to module README for more details.
