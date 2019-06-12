package org.hl7.davinci.pdex.refimpl.importer.mapper;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public class IdentifierMapper {

  private IGenericClient targetClient;

  public IdentifierMapper(IGenericClient targetClient) {
    this.targetClient = targetClient;
  }

  public void importNewIdentifiers(String patientId, Patient receivedPatient) {
    Patient targetPatient = targetClient.read()
        .resource(Patient.class)
        .withId(patientId)
        .execute();
    List<Identifier> targetIdentifiers = targetPatient.getIdentifier();

    for (Identifier receivedIdentifier : receivedPatient.getIdentifier()) {
      if (targetIdentifiers.stream()
          .noneMatch(identifier -> identifier.getSystem()
              .equals(receivedIdentifier.getSystem()))) {
        targetIdentifiers.add(receivedIdentifier);
      }
    }

    targetClient.update()
        .resource(targetPatient)
        .execute();
  }

}
