package org.hl7.davinci.pdex.refimpl.importer.mapper;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class LocationMapper {

  private TargetConfiguration targetConfiguration;

  LocationMapper(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
  }

  public Location readOrCreate(Reference receivedLocation, ImportRequest importRequest) {
    Location location = (Location) receivedLocation.getResource();
    if (location == null) {
      location = importRequest.getReceivedClient()
          .read()
          .resource(Location.class)
          .withId(receivedLocation.getReference())
          .execute();
    }
    return readOrCreate(location, importRequest);
  }

  Location readOrCreate(Location receivedLocation, ImportRequest importRequest) {
    Optional<Identifier> first = receivedLocation.getIdentifier()
        .stream()
        .filter(identifier -> targetConfiguration.getNpiSystem()
            .equals(identifier.getSystem()))
        .findFirst();

    Location targetLocation;
    if (first.isPresent()) {
      targetLocation = findByNPI(importRequest, first.get());
    } else {
      targetLocation = findByReceivedIdentifier(receivedLocation.getId(), importRequest);
    }
    if (targetLocation == null) {
      targetLocation = createLocation(receivedLocation, importRequest, first);
    }
    return targetLocation;
  }

  private Location createLocation(Location receivedLocation, ImportRequest importRequest, Optional<Identifier> first) {
    Location targetLocation;
    List<Identifier> identifiers = new ArrayList<>();
    if (first.isPresent()) {
      identifiers.add(first.get());
    }
    identifiers.add(new Identifier().setSystem(importRequest.getReceivedSystem())
        .setValue(receivedLocation.getId()));
    Location organizationToCreate = new Location().setIdentifier(identifiers);
    targetLocation = (Location) importRequest.getTargetClient()
        .create()
        .resource(organizationToCreate)
        .execute()
        .getResource();
    return targetLocation;
  }

  private Location findByReceivedIdentifier(String receivedPractitionerId, ImportRequest importRequest) {
    List<Bundle.BundleEntryComponent> entry = importRequest.getTargetClient()
        .search()
        .forResource(Location.class)
        .where(Location.IDENTIFIER.exactly()
            .systemAndIdentifier(importRequest.getReceivedSystem(), receivedPractitionerId))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!entry.isEmpty()) {
      return (Location) entry.get(0)
          .getResource();
    }
    return null;
  }

  private Location findByNPI(ImportRequest importRequest, Identifier identifier) {
    List<Bundle.BundleEntryComponent> entry = importRequest.getTargetClient()
        .search()
        .forResource(Location.class)
        .where(Location.IDENTIFIER.exactly()
            .systemAndIdentifier(targetConfiguration.getNpiSystem(), identifier.getValue()))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!entry.isEmpty()) {
      return (Location) entry.get(0)
          .getResource();
    } else {
      return null;
    }
  }
}
