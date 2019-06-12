package org.hl7.davinci.pdex.refimpl.importer.mapper;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class PractitionerMapper {

  private TargetConfiguration targetConfiguration;

  PractitionerMapper(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
  }

  Practitioner readOrCreate(Reference practitioner, ImportRequest importRequest) {
    Practitioner practitionerResource = (Practitioner) practitioner.getResource();
    if (practitionerResource == null) {
      practitionerResource = importRequest.getReceivedClient()
          .read()
          .resource(Practitioner.class)
          .withId(practitioner.getReference())
          .execute();
    }
    return readOrCreate(practitionerResource, importRequest);
  }

  Practitioner readOrCreate(Practitioner receivedPractitioner, ImportRequest importRequest) {
    Optional<Identifier> first = receivedPractitioner.getIdentifier()
        .stream()
        .filter(identifier -> targetConfiguration.getNpiSystem()
            .equals(identifier.getSystem()))
        .findFirst();

    Practitioner targetPractitioner;
    if (first.isPresent()) {
      targetPractitioner = findByNPI(importRequest, first.get());
    } else {
      targetPractitioner = findByReceivedIdentifier(receivedPractitioner.getId(), importRequest);
    }
    if (targetPractitioner == null) {
      targetPractitioner = createNewPractitioner(receivedPractitioner, importRequest, first);
    }
    return targetPractitioner;
  }

  private Practitioner createNewPractitioner(Practitioner receivedPractitioner, ImportRequest importRequest,
      Optional<Identifier> first) {
    Practitioner targetPractitioner;
    List<Identifier> identifiers = new ArrayList<>();
    if (first.isPresent()) {
      identifiers.add(first.get());
    }
    identifiers.add(new Identifier().setSystem(importRequest.getReceivedSystem())
        .setValue(receivedPractitioner.getId()));
    Practitioner organizationToCreate = new Practitioner().setIdentifier(identifiers);
    targetPractitioner = (Practitioner) importRequest.getTargetClient()
        .create()
        .resource(organizationToCreate)
        .execute()
        .getResource();
    return targetPractitioner;
  }

  private Practitioner findByReceivedIdentifier(String receivedPractitionerId, ImportRequest importRequest) {
    List<Bundle.BundleEntryComponent> entry = importRequest.getTargetClient()
        .search()
        .forResource(Practitioner.class)
        .where(Practitioner.IDENTIFIER.exactly()
            .systemAndIdentifier(importRequest.getReceivedSystem(), receivedPractitionerId))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!entry.isEmpty()) {
      return (Practitioner) entry.get(0)
          .getResource();
    }
    return null;
  }

  private Practitioner findByNPI(ImportRequest importRequest, Identifier identifier) {
    List<Bundle.BundleEntryComponent> entry = importRequest.getTargetClient()
        .search()
        .forResource(Practitioner.class)
        .where(Practitioner.IDENTIFIER.exactly()
            .systemAndIdentifier(targetConfiguration.getNpiSystem(), identifier.getValue()))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!entry.isEmpty()) {
      return (Practitioner) entry.get(0)
          .getResource();
    } else {
      return null;
    }
  }

}
