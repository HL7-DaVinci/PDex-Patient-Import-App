package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.importer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.ElementUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.ImportRecordDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.fhir.DisplayUtil;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.fhir.IGenericClientProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
//todo extract to separate module
public class Importer {

  private final IGenericClientProvider clientProvider;
  private final IParser parser;
  private final List<String> excludedResources;

  public Importer(
      @Autowired IGenericClientProvider clientProvider,
      @Autowired IParser parser,
      @Value("${payer-a.identifier.system}") String payerAIdentifierSystem,
      @Value("${payer-b.data-import.exclude-resources}") String excludeResourcesString
  ) {
    this.clientProvider = clientProvider;
    this.parser = parser;
    this.excludedResources = excludeResourcesString.isEmpty() ? new ArrayList<>() : Arrays.asList(
        excludeResourcesString.split(","));
  }

  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(
      String subscriberId,
      String payerServerUrl,
      String payerServerToken
  ) {
    IGenericClient payerAClient = clientProvider.client(payerServerUrl, payerServerToken);

    Parameters outParams = payerAClient.operation()
        .onInstance(new IdDt(subscriberId))
        .named("$everything")
        .withNoParameters(Parameters.class).execute();

    Bundle b = (Bundle) outParams.getParameter().get(0).getResource();
    Patient patient = (Patient)b.getEntry().get(0).getResource();

    Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();


    Bundle page = b;
    while (page != null){

      for (BundleEntryComponent bc : page.getEntry()){

        Resource r = bc.getResource();
        if(r == patient){
          continue;
        }
        Set<ImportRecordDto> importRecordDtos = importRecords.computeIfAbsent(r.getClass(), k -> new HashSet<>());
        importRecordDtos.add(new ImportRecordDto(r.getIdElement().getIdPart(), DisplayUtil.getDisplay(r)));
      }

      page = getNextBundle(payerAClient, page);
    }
    return importRecords;
  }

  public void importRecords(Map<Class<? extends Resource>, Set<String>> importIds, String subscriberId, String patientId, String payerServerUrl, String payerServerToken) {
    IGenericClient payerAClient = clientProvider.client(payerServerUrl, payerServerToken);
    IGenericClient payerBClient = clientProvider.client();

    Parameters outParams = payerAClient.operation()
        .onInstance(new IdDt("Patient/" + subscriberId))
        .named("$everything")
        .withNoParameters(Parameters.class).execute();

    CapabilityStatement capabilityStatementB = payerBClient.capabilities().ofType(CapabilityStatement.class).execute();

    Bundle firstPage = (Bundle) outParams.getParameter().get(0).getResource();
    Patient patient = (Patient)firstPage.getEntry().get(0).getResource();

    Bundle page = firstPage;
    while (page != null){

      Bundle persistBundle = new Bundle();
      persistBundle.setType(BundleType.TRANSACTION);
      Bundle documentBundle = new Bundle();
      persistBundle.setType(BundleType.TRANSACTION);

      for (BundleEntryComponent bc : page.getEntry()){

        Resource resource = bc.getResource();
        if(resource == patient){
          continue;
        }

        //well
        //Set id as null to create a new one on persist.
        resource.setId((String) null);

        //Reset old patient reference in Payer system to a new one from EMR
        setPatientReference(resource, patient);
        //we agreed that for RI it is sufficient handling
        cutInvalidReferences(resource);
        //

        if(canPersist(capabilityStatementB, resource.getClass().getSimpleName())){
          System.out.println("Adding to bundle " + resource.getClass().getSimpleName());
          persistBundle.addEntry().setResource(resource).getRequest().setMethod(Bundle.HTTPVerb.POST);
        }else {
          System.out.println("Adding to document ref bundle  " + resource.getClass().getSimpleName());
          documentBundle.addEntry()
              .setResource(resource)
              .getRequest()
              .setMethod(Bundle.HTTPVerb.POST);
        }

      }
      if (!persistBundle.getEntry().isEmpty()) {
        System.out.println("Persisting bundle of size " + persistBundle.getEntry().size() );
        payerBClient.transaction().withBundle(persistBundle).execute();
      }
      if (!documentBundle.getEntry().isEmpty()) {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        documentReference.setSubject(new Reference("Patient/" + patient.getIdElement().getIdPart()));
        DocumentReference.DocumentReferenceContentComponent content =
            new DocumentReference.DocumentReferenceContentComponent();
        content.setAttachment(new Attachment().setData(parser.encodeResourceToString(documentBundle).getBytes()));
        documentReference.addContent( );
        payerBClient.create().resource(documentReference).execute();
      }

      page = getNextBundle(payerAClient, page);
    }

  }

  private Bundle getNextBundle(IGenericClient payerAClient, Bundle page) {
    if(page.getLink(Bundle.LINK_NEXT) != null){
      page = payerAClient.loadPage()
          .next(page)
          .execute();
    }else{
      page = null;
    }
    return page;
  }

  private boolean canPersist(CapabilityStatement capabilityStatement, String resourceName) {
    return checkCapabilityStatementFor(capabilityStatement, resourceName, "create");
  }

  private boolean checkCapabilityStatementFor(CapabilityStatement capabilityStatement, String resourceName,
      String action) {
    Optional<CapabilityStatementRestResourceComponent> res = capabilityStatement.getRest().get(0).getResource().stream()
        .filter(c -> resourceName.equals(c.getType())).findFirst();

    if (res.isPresent() && !excludedResources.contains(resourceName)) {
      Optional<ResourceInteractionComponent> status = res.get().getInteraction().stream().filter(
          i -> action.equals(i.getCode().getDisplay())).findFirst();
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
    }
    else if (resource.getClass() == Observation.class) {
      ((Observation) resource).setSubject(ref);
    } else if (resource.getClass() == DocumentReference.class) {
      ((DocumentReference) resource).setSubject(ref);
    } else if (resource.getClass() == Coverage.class) {
      //((Coverage) resource).set(ref);
      } else if (resource.getClass() == Organization.class) {
      // todo link all together
    } else if (resource.getClass() == MedicationRequest.class) {
      ((MedicationRequest) resource).setSubject(ref);
    }else {
      System.out.println("Setting Patient reference not supported for type" + resource.getClass());
      //throw new NotImplementedException("Setting Patient reference not supported for type " + resource.getClass());
    }
  }

  private void cutInvalidReferences(Resource resource) {
    if (resource.getClass() == Encounter.class) {
      Encounter encounter = (Encounter) resource;
      encounter.getParticipant().forEach(loc -> loc.setIndividual(new Reference()));
      encounter.getLocation().forEach(loc -> loc.setLocation(new Reference()));
      encounter.setServiceProvider(new Reference());
    }
    if (resource.getClass() == Coverage.class) {
      Coverage coverage = (Coverage) resource;
      coverage.setPayor(Collections.emptyList());
    }
  }
}


