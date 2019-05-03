package com.healthlx.demo.pdex2019.provider.service;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthlx.demo.pdex2019.provider.dto.ImportRecordDto;
import com.healthlx.demo.pdex2019.provider.fhir.DisplayUtil;
import com.healthlx.demo.pdex2019.provider.fhir.IGenericClientProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DataImportService {

  @Autowired
  private IGenericClientProvider clientProvider;

  @Autowired
  private IParser parser;

  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(String subscriberId,
      String payerServerUrl, String payerServerToken) {
    IGenericClient payerClient = clientProvider.client(payerServerUrl, payerServerToken);

    Patient patient = payerClient.read().resource(Patient.class).withId(subscriberId).execute();

    Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();

    //List<Bundle> bundles =
    //Search Encounters
    //TODO search by date and exclude serviceProvider
    //Retrieve only IDs and data for displays. We do not need anything else here.
    Bundle encounters = payerClient.search().forResource(Encounter.class).where(
        Encounter.SUBJECT.hasId(patient.getIdElement().getIdPart())).returnBundle(Bundle.class).execute();
    importRecords.put(Encounter.class, bundleToImportRecords(encounters));

    //Search Procedures
    Bundle procedures = payerClient.search().forResource(Procedure.class).where(
        Procedure.SUBJECT.hasId(patient.getIdElement().getIdPart())).returnBundle(Bundle.class).execute();
    importRecords.put(Procedure.class, bundleToImportRecords(procedures));

    //Search Medication Dispense
    Bundle medicationDispenses = payerClient.search().forResource(MedicationDispense.class).where(
        MedicationDispense.SUBJECT.hasId(patient.getIdElement().getIdPart())).returnBundle(Bundle.class).execute();
    importRecords.put(MedicationDispense.class, bundleToImportRecords(medicationDispenses));

    return importRecords;

  }

  private HashSet<ImportRecordDto> bundleToImportRecords(Bundle bundle) {
    HashSet<ImportRecordDto> records = new HashSet<>();
    for (BundleEntryComponent entry : bundle.getEntry()) {
      records.add(new ImportRecordDto(entry.getResource().getIdElement().getIdPart(),
          DisplayUtil.getDisplay(entry.getResource())));
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

    for (Class<? extends Resource> rClass : importIds.keySet()) {
      Set<String> ids = importIds.get(rClass);
      if (ids == null) {
        continue;
      }

      //Currently no _include is used. We should retrieve all referenced objects as contained resources.
      //This is just a proof of concept to show that we can copy resources from Payer to Provider.
      Bundle bundle = payerClient.search().forResource(rClass).where(
          Resource.RES_ID.exactly().codes(importIds.get(rClass))).returnBundle(Bundle.class).execute();

      boolean canPersist = canPersist(capabilityStatement, rClass);
      for (BundleEntryComponent entry : bundle.getEntry()) {
        Resource r = entry.getResource();
        //Set id as null to create a new one on persist.
        r.setId((String) null);

        //Reset old patient reference in Payer system to a new one from EMR
        setPatientReference(r, patient);
        if (canPersist) {
          persistBundle.addEntry().setResource(r).getRequest().setMethod(Bundle.HTTPVerb.POST);
        } else {
          documentBundle.addEntry().setResource(r).getRequest().setMethod(Bundle.HTTPVerb.POST);
        }
      }
    }
    if (persistBundle.getEntry().size() != 0) {
      client.transaction().withBundle(persistBundle).execute();
    }
    if (documentBundle.getEntry().size() != 0) {
      DocumentReference.DocumentReferenceContentComponent c = new DocumentReference.DocumentReferenceContentComponent();
      c.setAttachment(new Attachment().setData(parser.encodeResourceToString(documentBundle).getBytes()));
      DocumentReference dr = new DocumentReference();
      dr.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
      dr.addContent(c);
      dr.setSubject(new Reference(patient));
      MethodOutcome out = client.create().resource(dr).execute();
      System.out.println(parser.encodeResourceToString(out.getResource()));
    }
  }

  private boolean canPersist(CapabilityStatement capabilityStatement, Class<? extends Resource> rClass) {
    Optional<CapabilityStatementRestResourceComponent> res = capabilityStatement.getRest().get(0).getResource().stream()
        .filter(c -> rClass.getSimpleName().equals(c.getType())).findFirst();
    if (res.isPresent()) {
      Optional<ResourceInteractionComponent> status = res.get().getInteraction().stream().filter(
          i -> "create".equals(i.getCode().getDisplay())).findFirst();
      if (status.isPresent()) {
        return true;
      }
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


