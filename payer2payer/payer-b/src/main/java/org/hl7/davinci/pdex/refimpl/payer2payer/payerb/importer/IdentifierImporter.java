package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.importer;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

class IdentifierImporter {

  IGenericClient aClient;
  IGenericClient bClient;

  IdentifierImporter(IGenericClient aClient, IGenericClient bClient) {
    this.aClient = aClient;
    this.bClient = bClient;
  }

  void importNewIdentifiers(String patientId, String subscriberId) {
    Patient payerBPatient = bClient.read()
        .resource(Patient.class)
        .withId(patientId)
        .execute();
    List<Identifier> bIdentifiers = payerBPatient.getIdentifier();

    Patient payerAPatient = aClient.read()
        .resource(Patient.class)
        .withId(subscriberId)
        .execute();

    for (Identifier aIdentifier : payerAPatient.getIdentifier()) {
      if (bIdentifiers.stream()
          .noneMatch(identifier -> identifier.getSystem()
              .equals(aIdentifier.getSystem()))) {
        bIdentifiers.add(aIdentifier);
      }
    }

    bClient.update()
        .resource(payerBPatient)
        .execute();
  }

}
