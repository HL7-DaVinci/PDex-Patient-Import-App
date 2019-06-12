package org.hl7.davinci.pdex.refimpl.importer.mapper;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class OrganizationMapper {

  private TargetConfiguration targetConfiguration;

  OrganizationMapper(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
  }

  /**
   * Method searches for corresponding Organization using NPI and resource ID. If nothing found organization is created.
   * In case resource is not contained it should be preloaded.
   */
  Organization readOrCreate(Reference receivedOrganizationRef, ImportRequest importRequest) {
    Organization resource = (Organization) receivedOrganizationRef.getResource();
    if (resource == null) {
      resource = importRequest.getReceivedClient()
          .read()
          .resource(Organization.class)
          .withId(receivedOrganizationRef.getId())
          .execute();
    }
    return readOrCreate(resource, importRequest);
  }

  /**
   * Method searches for corresponding Organization using NPI and resource ID. If nothing found organization is
   * created.
   */
  Organization readOrCreate(Organization receivedOrganization, ImportRequest importRequest) {
    Optional<Identifier> first = receivedOrganization.getIdentifier()
        .stream()
        .filter(identifier1 -> targetConfiguration.getNpiSystem()
            .equals(identifier1.getSystem()))
        .findFirst();

    Organization targetOrganization;
    if (first.isPresent()) {
      targetOrganization = lookByNpi(importRequest, first.get());
    } else {
      targetOrganization = lookByIdentifiers(receivedOrganization.getId(), importRequest);
    }
    if (targetOrganization == null) {
      targetOrganization = createOrganization(receivedOrganization, importRequest, first);
    }
    return targetOrganization;
  }

  private Organization createOrganization(Organization receivedOrganization, ImportRequest importRequest,
      Optional<Identifier> first) {
    Organization targetOrganization;
    List<Identifier> identifiers = new ArrayList<>();
    if (first.isPresent()) {
      identifiers.add(first.get());
    }
    identifiers.add(new Identifier().setSystem(importRequest.getReceivedSystem())
        .setValue(receivedOrganization.getId()));
    Organization organizationToCreate = new Organization().setIdentifier(identifiers);
    targetOrganization = (Organization) importRequest.getTargetClient()
        .create()
        .resource(organizationToCreate)
        .execute()
        .getResource();
    return targetOrganization;
  }

  private Organization lookByNpi(ImportRequest importRequest, Identifier identifier) {
    List<Bundle.BundleEntryComponent> bundleEntryComponent = importRequest.getTargetClient()
        .search()
        .forResource(Organization.class)
        .where(Organization.IDENTIFIER.exactly()
            .systemAndIdentifier(targetConfiguration.getNpiSystem(), identifier.getValue()))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!bundleEntryComponent.isEmpty()) {
      return (Organization) bundleEntryComponent.get(0)
          .getResource();
    } else {
      return null;
    }
  }

  private Organization lookByIdentifiers(String receivedOrganizationId, ImportRequest importRequest) {
    List<Bundle.BundleEntryComponent> bundleEntryComponent = importRequest.getTargetClient()
        .search()
        .forResource(Organization.class)
        .where(Organization.IDENTIFIER.exactly()
            .systemAndIdentifier(importRequest.getReceivedSystem(), receivedOrganizationId))
        .returnBundle(Bundle.class)
        .execute()
        .getEntry();
    if (!bundleEntryComponent.isEmpty()) {
      return (Organization) bundleEntryComponent.get(0)
          .getResource();
    }
    return null;
  }

}