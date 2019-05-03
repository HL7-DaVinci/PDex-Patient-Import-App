package com.healthlx.demo.pdex2019.provider.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsRequest;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsResponse;
import com.healthlx.demo.pdex2019.cdshooks.model.FhirAuthorization;
import com.healthlx.demo.pdex2019.provider.dto.CurrentContextDto;
import com.healthlx.demo.pdex2019.provider.dto.CurrentContextResponseDto;
import com.healthlx.demo.pdex2019.provider.fhir.FhirResourceNotFoundException;
import com.healthlx.demo.pdex2019.provider.fhir.IGenericClientProvider;
import com.healthlx.demo.pdex2019.provider.oauth2.context.OAuth2ClientContextHolder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CallHookService {

  @Autowired
  private IGenericClientProvider clientProvider;

  @Value("${payer.cds-hook-uri}")
  private String cdsHookUri;

  public CurrentContextResponseDto getCurrentContextDetails(CurrentContextDto currentContext) {
    IGenericClient client = clientProvider.client();
    Patient patient = client.read().resource(Patient.class).withId(currentContext.getPatientId()).execute();
    Practitioner practitioner = client.read().resource(Practitioner.class).withId(currentContext.getUserId()).execute();
    Encounter encounter = Optional.ofNullable(currentContext.getEncounterId()).map(a -> client.read().resource(
        Encounter.class).withId(a).execute()).orElse(null);
    List<Coverage> coverages = client.search().forResource(Coverage.class).where(
        Coverage.SUBSCRIBER.hasId(patient.getIdElement().getIdPart())).and(Coverage.STATUS.exactly().code("active"))
        .include(Coverage.INCLUDE_PAYOR.asNonRecursive()).returnBundle(Bundle.class).execute().getEntry().stream().map(
            BundleEntryComponent::getResource).filter(Coverage.class::isInstance).map(Coverage.class::cast).collect(
            Collectors.toList());

    return new CurrentContextResponseDto(patient, practitioner, encounter, coverages);
  }

  //TODO: Select a CDS Hook Service URL based on a Coverage->Payor. Currently all requests will go to the same Payer
  // CDS Hook service. Even if no Coverage is present - we will just send a CDS Hook request withou a subscriber ID.
  public CdsResponse callHook(String patientId, String practitionerId, String encounterId, String coverageId)
      throws FhirResourceNotFoundException {

    Assert.notNull(patientId, "Patient ID cannot be null");
    Assert.notNull(practitionerId, "Practitioner ID cannot be null");
    //Disabling this check will let us launch and test the App without encounter selection.
    //Assert.notNull(encounterId, "Encounter ID cannot be null");

    //Coverage can be null?
    //Assert.notNull(coverageId, "Coverage ID cannot be null");

    IGenericClient client = clientProvider.client();

    //Retrieve resources to check whether IDs are valid
    Patient patient = client.read().resource(Patient.class).withId(patientId).execute();
    Practitioner practitioner = client.read().resource(Practitioner.class).withId(practitionerId).execute();
    Encounter encounter = Optional.ofNullable(encounterId).map(a -> client.read().resource(Encounter.class).withId(a)
        .execute()).orElse(null);
    Coverage coverage = Optional.ofNullable(coverageId).map(a -> client.read().resource(Coverage.class).withId(a)
        .execute()).orElse(null);

    //Check resources exist
    String verifiedPatientId = Optional.ofNullable(patient).map(p -> patient.getIdElement().getIdPart()).orElseThrow(
        () -> new FhirResourceNotFoundException(patientId, Patient.class));

    String verifiedPractitionerId = Optional.ofNullable(practitioner).map(p -> practitioner.getIdElement().getIdPart())
        .orElseThrow(() -> new FhirResourceNotFoundException(practitionerId, Practitioner.class));

    //Disabling null check for encounter will let us launch and test the App without encounter selection.
    String verifiedEncounterId = Optional.ofNullable(encounter).map(p -> encounter.getIdElement().getIdPart()).orElse(
        null);
    String subscriberId = Optional.ofNullable(coverage).map(p -> coverage.getSubscriberId()).orElse(null);

    CdsRequest r = composeCdsRequest(client, verifiedPatientId, verifiedPractitionerId, verifiedEncounterId,
        subscriberId);

    RestTemplate t = new RestTemplate();
    return t.postForEntity(cdsHookUri, r, CdsResponse.class).getBody();
  }

  private CdsRequest composeCdsRequest(IGenericClient client, String patientId, String practitionerId,
      String encounterId, String subscriberId) {
    CdsRequest r = new CdsRequest();
    r.setHook("appointment-book");
    r.setFhirServer(client.getServerBase());

    FhirAuthorization fa = new FhirAuthorization();
    fa.setAccessToken(OAuth2ClientContextHolder.currentContext().getAccessToken().getValue());
    fa.setTokenType(OAuth2ClientContextHolder.currentContext().getAccessToken().getTokenType());
    r.setFhirAuthorization(fa);

    r.setUser(practitionerId);
    r.setPatient(patientId);
    r.setContext(new LinkedHashMap<>());
    r.getContext().put("userId", practitionerId);
    r.getContext().put("patientId", patientId);
    r.getContext().put("encounter", encounterId);
    r.getContext().put("appointments", new Object[] {});
    r.getContext().put("subscriberId", subscriberId);
    return r;
  }
}
