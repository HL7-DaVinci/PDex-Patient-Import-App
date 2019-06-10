package org.hl7.davinci.pdex.refimpl.importer.mapper;

import ca.uhn.fhir.rest.client.api.IGenericClient;
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

  public OrganizationMapper(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
  }

  /**
   * Method searches for corresponding Organization using NPI and resource ID. If nothing found organization is
   * created.
   */
  Organization readOrCreate(Organization receivedOrganization, ImportRequest importRequest, IGenericClient targetClient) {
    Optional<Identifier> first = receivedOrganization.getIdentifier()
        .stream()
        .filter(identifier1 -> targetConfiguration.getNpiSystem().equals(identifier1.getSystem()))
        .findFirst();

    Organization targetOrganization = null;
    if (first.isPresent()) {
      Identifier identifier = first.get();
      List<Bundle.BundleEntryComponent> bundleEntryComponent = targetClient.search()
          .forResource(Organization.class)
          .where(Organization.IDENTIFIER.exactly()
              .systemAndIdentifier(targetConfiguration.getNpiSystem(), identifier.getValue()))
          .returnBundle(Bundle.class)
          .execute()
          .getEntry();
      if (!bundleEntryComponent.isEmpty()) {
        targetOrganization = (Organization) bundleEntryComponent.get(0)
            .getResource();
      }
    } else {
      List<Bundle.BundleEntryComponent> bundleEntryComponent = targetClient.search()
          .forResource(Organization.class)
          .where(Organization.IDENTIFIER.exactly()
              .systemAndIdentifier(importRequest.getReceivedSystem(), receivedOrganization.getId()))
          .returnBundle(Bundle.class)
          .execute()
          .getEntry();
      if (!bundleEntryComponent.isEmpty()) {
        targetOrganization = (Organization) bundleEntryComponent.get(0)
            .getResource();
      }
    }
    if (targetOrganization == null) {
      List<Identifier> identifiers = new ArrayList<>();
      if (first.isPresent()) {
        identifiers.add(first.get());
      }
      identifiers.add(new Identifier().setSystem(importRequest.getReceivedSystem())
          .setValue(receivedOrganization.getId()));
      Organization organizationToCreate = new Organization().setIdentifier(identifiers);
      targetOrganization = (Organization) targetClient.create()
          .resource(organizationToCreate)
          .execute()
          .getResource();
    }
    return targetOrganization;
  }

  Organization readOrCreate(Reference receivedOrganization, ImportRequest importRequest, IGenericClient targetClient) {
    Organization readOrganization = importRequest.getReceivedClient().read()
        .resource(Organization.class)
        .withId(receivedOrganization.getReference())
        .execute();
    return readOrCreate(readOrganization, importRequest, targetClient);
  }

}