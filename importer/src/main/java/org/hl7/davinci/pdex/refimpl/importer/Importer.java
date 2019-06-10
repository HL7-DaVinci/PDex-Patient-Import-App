package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.davinci.pdex.refimpl.importer.mapper.ImportResourceMapper;
import org.hl7.davinci.pdex.refimpl.importer.presentation.DisplayUtil;
import org.hl7.davinci.pdex.refimpl.importer.presentation.ImportRecordDto;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceInteractionComponent;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Importer {

  private TargetConfiguration targetConfiguration;
  private ImportResourceMapper mapper;

  public Importer(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
    mapper = new ImportResourceMapper(targetConfiguration);
  }

  public void temporaryFix(IGenericClient client){
    //todo rewire DI
    targetConfiguration.setClient(client);
  }

  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsForImport(ImportRequest receivedConfiguration) {

    Parameters outParams = receivedConfiguration.getClient()
        .operation()
        .onInstance(new IdDt(receivedConfiguration.getSubscriverId()))
        .named("$everything")
        .withNoParameters(Parameters.class)
        .execute();

    Bundle b = (Bundle) outParams.getParameter()
        .get(0)
        .getResource();
    Patient patient = (Patient) b.getEntry()
        .get(0)
        .getResource();

    Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();

    Bundle page = b;
    while (page != null) {

      for (BundleEntryComponent bc : page.getEntry()) {

        Resource r = bc.getResource();
        if (r == patient) {
          continue;
        }
        Set<ImportRecordDto> importRecordDtos = importRecords.computeIfAbsent(r.getClass(), k -> new HashSet<>());
        importRecordDtos.add(new ImportRecordDto(r.getIdElement()
            .getIdPart(), DisplayUtil.getDisplay(r)));
      }

      page = getNextBundle(receivedConfiguration.getClient(), page);
    }
    return importRecords;
  }

  public void importRecords(ImportRequest importRequest) {

    Parameters outParams = importRequest.getClient()
        .operation()
        .onInstance(new IdDt(importRequest.getSubscriverId()))
        .named("$everything")
        .withNoParameters(Parameters.class)
        .execute();

    CapabilityStatement capabilityStatementB = targetConfiguration.getClient()
        .capabilities()
        .ofType(CapabilityStatement.class)
        .execute();

    Bundle firstPage = (Bundle) outParams.getParameter()
        .get(0)
        .getResource();
    Patient patient = (Patient) firstPage.getEntry()
        .get(0)
        .getResource();

    Bundle page = firstPage;
    while (page != null) {

      Bundle persistBundle = new Bundle();
      persistBundle.setType(BundleType.TRANSACTION);
      Bundle documentBundle = new Bundle();
      persistBundle.setType(BundleType.TRANSACTION);

      for (BundleEntryComponent bc : page.getEntry()) {

        Resource resource = bc.getResource();
        if (resource == patient) {
          continue;
        }

        mapResourceReferences(resource, importRequest);

        //Set id as null to create a new one on persist.
        resource.setId((String) null);

        if (canPersist(capabilityStatementB, resource.getClass()
            .getSimpleName())) {
          System.out.println("Adding to bundle " + resource.getClass()
              .getSimpleName());
          persistBundle.addEntry()
              .setResource(resource)
              .getRequest()
              .setMethod(Bundle.HTTPVerb.POST);
        } else {
          System.out.println("Adding to document ref bundle  " + resource.getClass()
              .getSimpleName());
          documentBundle.addEntry()
              .setResource(resource)
              .getRequest()
              .setMethod(Bundle.HTTPVerb.POST);
        }

      }

      if (!persistBundle.getEntry()
          .isEmpty()) {
        System.out.println("Persisting bundle of size " + persistBundle.getEntry()
            .size());

        try {
          targetConfiguration.getClient().transaction()
              .withBundle(persistBundle)
              .execute();
        } catch (InvalidRequestException invalidRequestException) {
          System.out.println(invalidRequestException.getMessage());
        }
      }
      if (!documentBundle.getEntry()
          .isEmpty()) {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        documentReference.setSubject(new Reference(patient));
        DocumentReference.DocumentReferenceContentComponent content =
            new DocumentReference.DocumentReferenceContentComponent();
        content.setAttachment(new Attachment().setData(targetConfiguration.getParser().encodeResourceToString(documentBundle)
            .getBytes()));
        documentReference.addContent();
        targetConfiguration.getClient().create()
            .resource(documentReference)
            .execute();
      }

      page = getNextBundle(importRequest.getClient(), page);
    }

  }

  private Bundle getNextBundle(IGenericClient payerAClient, Bundle page) {
    if (page.getLink(Bundle.LINK_NEXT) != null) {
      page = payerAClient.loadPage()
          .next(page)
          .execute();
    } else {
      page = null;
    }
    return page;
  }

  private boolean canPersist(CapabilityStatement capabilityStatement, String resourceName) {
    Optional<CapabilityStatementRestResourceComponent> res = capabilityStatement.getRest()
        .get(0)
        .getResource()
        .stream()
        .filter(c -> resourceName.equals(c.getType()))
        .findFirst();

    if (res.isPresent() && !targetConfiguration.getExcludedResources().contains(resourceName)) {
      Optional<ResourceInteractionComponent> status = res.get()
          .getInteraction()
          .stream()
          .filter(i -> "create".equals(i.getCode()
              .getDisplay()))
          .findFirst();
      return status.isPresent();
    }

    return false;
  }

  //We map here only simple references, in real use case ALL references should be mapped
  private void mapResourceReferences(Resource resource, ImportRequest importRequest) {
    Reference patientRef = new Reference(importRequest.getPatientId());
    if (resource.getClass() == Procedure.class) {
      ((Procedure) resource).setSubject(patientRef);
    } else if (resource.getClass() == Encounter.class) {
      Encounter encounter = (Encounter) resource;
      encounter.setSubject(patientRef);
      encounter.getParticipant()
          .forEach(loc -> loc.setIndividual(new Reference()));
      encounter.getLocation()
          .forEach(loc -> loc.setLocation(new Reference()));

      encounter.setServiceProvider(new Reference(mapper.map(encounter.getServiceProvider(),importRequest)));
    } else if (resource.getClass() == MedicationDispense.class) {
      ((MedicationDispense) resource).setSubject(patientRef);
    } else if (resource.getClass() == Observation.class) {
      Observation observation = (Observation) resource;
      observation.setSubject(patientRef);
    } else if (resource.getClass() == DocumentReference.class) {
      ((DocumentReference) resource).setSubject(patientRef);
    } else if (resource.getClass() == Coverage.class) {
      Coverage coverage = ((Coverage) resource);
      coverage.setSubscriber(patientRef);
      List<Reference> newReferences = new ArrayList<>();
      for (Reference reference : coverage.getPayor()) {
        newReferences.add(new Reference(mapper.map(reference, importRequest)));
      }
      coverage.setPayor(newReferences);

    } else if (resource.getClass() == Organization.class) {
      mapper.map(resource, importRequest);
    } else if (resource.getClass() == MedicationRequest.class) {
      ((MedicationRequest) resource).setSubject(patientRef);
    } else {
      System.out.println("Mapping references not supported for type" + resource.getClass());
      //throw new NotImplementedException("Setting Patient reference not supported for type " + resource.getClass());
      // todo remove ths temporary stub
    }
  }
}


