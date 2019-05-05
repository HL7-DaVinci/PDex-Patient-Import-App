package com.healthlx.demo.pdex2019.provider.service;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthlx.demo.pdex2019.provider.dto.ImportRecordDto;
import com.healthlx.demo.pdex2019.provider.fhir.DisplayUtil;
import com.healthlx.demo.pdex2019.provider.fhir.IGenericClientProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceInteractionComponent;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class DataImportService {

  private final IGenericClientProvider clientProvider;
  private final IParser parser;
  private final List<String> excludedResources;

  public DataImportService(@Autowired IGenericClientProvider clientProvider, @Autowired IParser parser,
      @Value("${provider.data-import.exclude-resources}") String excludeResourcesString) {
    this.clientProvider = clientProvider;
    this.parser = parser;
    this.excludedResources = excludeResourcesString.isEmpty() ? new ArrayList<>() : Arrays.asList(
        excludeResourcesString.split(","));
  }

  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(String subscriberId,
      String payerServerUrl, String payerServerToken) {
    IGenericClient payerClient = clientProvider.client(payerServerUrl, payerServerToken);
    Patient patient = payerClient.read().resource(Patient.class).withId(subscriberId).execute();
    Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();

    //List<Bundle> bundles =
    //Search Encounters
    //TODO search by date and exclude serviceProvider
    //Retrieve only IDs and data for displays. We do not need anything else here.

    String patientIdPart = patient.getIdElement().getIdPart();

    Bundle encounters = payerClient.search().forResource(Encounter.class).where(Encounter.SUBJECT.hasId(patientIdPart))
        .returnBundle(Bundle.class).execute();
    importRecords.put(Encounter.class, bundleToImportRecords(encounters));

    //Search Procedures
    Bundle procedures = payerClient.search().forResource(Procedure.class).where(Procedure.SUBJECT.hasId(patientIdPart))
        .returnBundle(Bundle.class).execute();
    importRecords.put(Procedure.class, bundleToImportRecords(procedures));

    //Search Medication Dispense
    Bundle medicationDispenses = payerClient.search().forResource(MedicationDispense.class).where(
        MedicationDispense.SUBJECT.hasId(patientIdPart)).returnBundle(Bundle.class).execute();
    importRecords.put(MedicationDispense.class, bundleToImportRecords(medicationDispenses));

    return importRecords;
  }

  private HashSet<ImportRecordDto> bundleToImportRecords(Bundle bundle) {
    HashSet<ImportRecordDto> records = new HashSet<>();
    for (BundleEntryComponent entry : bundle.getEntry()) {
      String resourceIdPart = entry.getResource().getIdElement().getIdPart();
      records.add(new ImportRecordDto(resourceIdPart, DisplayUtil.getDisplay(entry.getResource())));
    }
    return records;
  }

  public void importRecords(Map<Class<? extends Resource>, Set<String>> importIds, String patientId,
      String payerServerUrl, String payerServerToken) {
    IGenericClient client = clientProvider.client();
    Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
    CapabilityStatement capabilityStatement = client.capabilities().ofType(CapabilityStatement.class).execute();

    IGenericClient payerClient = clientProvider.client(payerServerUrl, payerServerToken);

    //Bundle to persist data to EMR
    Bundle persistBundle = new Bundle();
    persistBundle.setType(BundleType.TRANSACTION);

    //Bundle to be serialized and stored as a Document Reference
    Bundle documentBundle = new Bundle();
    persistBundle.setType(BundleType.TRANSACTION);

    for (Map.Entry<Class<? extends Resource>, Set<String>> idsEntry : importIds.entrySet()) {
      if (idsEntry.getValue() == null) {
        continue;
      }
      Class<? extends Resource> rClass = idsEntry.getKey();
      //Currently no _include is used. We should retrieve all referenced objects as contained resources.
      //This is just a proof of concept to show that we can copy resources from Payer to Provider.
      Bundle bundle = payerClient.search().forResource(rClass).where(
          Resource.RES_ID.exactly().codes(importIds.get(rClass))).returnBundle(Bundle.class).execute();

      boolean canPersist = canPersist(capabilityStatement, rClass.getSimpleName());
      for (BundleEntryComponent entry : bundle.getEntry()) {
        Resource resource = entry.getResource();
        //Set id as null to create a new one on persist.
        resource.setId((String) null);

        //Reset old patient reference in Payer system to a new one from EMR
        setPatientReference(resource, patient);
        if (canPersist) {
          persistBundle.addEntry().setResource(resource).getRequest().setMethod(Bundle.HTTPVerb.POST);
        } else {
          documentBundle.addEntry().setResource(resource).getRequest().setMethod(Bundle.HTTPVerb.POST);
        }
      }
    }
    if (!persistBundle.getEntry().isEmpty()) {
      client.transaction().withBundle(persistBundle).execute();
    }
    if (!documentBundle.getEntry().isEmpty()) {
      DocumentReference documentReference = new DocumentReference();
      documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
      documentReference.setSubject(new Reference(patient));
      DocumentReference.DocumentReferenceContentComponent content =
          new DocumentReference.DocumentReferenceContentComponent();
      content.setAttachment(new Attachment().setData(parser.encodeResourceToString(documentBundle).getBytes()));
      documentReference.addContent(content);
      client.create().resource(documentReference).execute();
    }
  }

  private boolean canPersist(CapabilityStatement capabilityStatement, String resourceName) {
    Optional<CapabilityStatementRestResourceComponent> res = capabilityStatement.getRest().get(0).getResource().stream()
        .filter(c -> resourceName.equals(c.getType())).findFirst();
    if (res.isPresent() && !excludedResources.contains(resourceName)) {
      Optional<ResourceInteractionComponent> status = res.get().getInteraction().stream().filter(
          i -> "create".equals(i.getCode().getDisplay())).findFirst();
      return status.isPresent();
    }
    return false;
  }

  private void setPatientReference(Resource resource, Patient patient) {
    Reference ref = new Reference("Patient/" + patient.getIdElement().getIdPart());
    if (resource.getClass() == Procedure.class) {
      ((Procedure) resource).setSubject(ref);
    } else if (resource.getClass() == Encounter.class) {
      ((Encounter) resource).setSubject(ref);
    } else if (resource.getClass() == MedicationDispense.class) {
      ((MedicationDispense) resource).setSubject(ref);
    } else {
      throw new NotImplementedException("Setting Patient reference not supported for type " + resource.getClass());
    }
  }
}


