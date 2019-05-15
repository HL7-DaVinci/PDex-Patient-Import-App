package com.healthlx.demo.pdex2019.payer.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthlx.demo.pdex2019.cdshooks.model.Card;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsRequest;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsService;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsServiceInformation;
import com.healthlx.demo.pdex2019.cdshooks.model.Link;
import com.healthlx.demo.pdex2019.payer.exception.CdsServiceNotFoundException;
import com.healthlx.demo.pdex2019.payer.exception.CdsServiceNotSupportedException;
import com.healthlx.demo.pdex2019.payer.exception.PatientNotFoundException;
import com.healthlx.demo.pdex2019.payer.exception.PatientNotUniqueException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class CdsHooksService {

  private final FhirContext fhirContext;
  private final IGenericClient payerFhirClient;
  private final String payerFhirServer;
  private final String dataImportUri;

  public CdsHooksService(@Autowired FhirContext fhirContext, @Autowired IGenericClient payerFhirClient,
      @Value("${payer.fhir-server-uri}") String payerFhirServer,
      @Value("${patient-data-import.smart-app-uri}") String dataImportUri) {
    this.fhirContext = fhirContext;
    this.payerFhirServer = payerFhirServer;
    this.dataImportUri = dataImportUri;
    this.payerFhirClient = payerFhirClient;
  }

  public CdsServiceInformation getCdsServices() {
    CdsServiceInformation cdsServiceInformation = new CdsServiceInformation();
    cdsServiceInformation.addServicesItem(new SmartAppointmentHook());
    return cdsServiceInformation;
  }

  public Card createCard(String hookId, CdsRequest cdsRequest) {
    Optional<CdsService> cdsService = getCdsServices().getServices().stream().filter(
        service -> Objects.equals(service.getId(), hookId)).findFirst();
    if (!cdsService.isPresent()) {
      throw new CdsServiceNotFoundException("CDS Service with id '" + hookId + "' was not found.");
    }
    if (cdsService.get() instanceof SmartAppointmentHook) {
      return createAppointmentCard(cdsRequest);
    }
    throw new CdsServiceNotSupportedException("CDS service with id '" + hookId + "' is not supported by the system.");
  }

  public Card createSimpleCard(String summary) {
    Card card = new Card();
    card.setIndicator(Card.IndicatorEnum.INFO);
    card.setSummary(summary);
    return card;
  }

  private Card createAppointmentCard(CdsRequest cdsRequest) {
    String fhirServer = cdsRequest.getFhirServer();
    Assert.notNull(fhirServer, "FHIR Server location is missing.");
    Object patientId = cdsRequest.getContext().get("patientId");
    Assert.notNull(patientId, "PatientId is missing in the context.");
    Patient payerPatient;
    Object subscriberId = cdsRequest.getContext().get("subscriberId");
    if (subscriberId != null) {
      payerPatient = readPatient(payerFhirClient, subscriberId.toString());
    } else {
      Patient providerPatient = readPatient(
          createFhirClient(fhirServer, cdsRequest.getFhirAuthorization().getAccessToken()), patientId.toString());
      payerPatient = readPatientByDemographics(providerPatient);
    }
    Card card = createSimpleCard("Import Patient Data through a SMART-App.");
    Link link = new Link();
    link.setUrl(dataImportUri + "?payerServerUrl=" + payerFhirServer + "&subscriberId=" + payerPatient.getIdElement()
        .getIdPart() + "&patientId=" + patientId.toString());
    link.setLabel("Import data");
    card.addLinksItem(link);
    return card;
  }

  private Patient readPatientByDemographics(Patient providerPatient) {
    List<String> familyNames = providerPatient.getName().stream().map(HumanName::getFamily).collect(
        Collectors.toList());
    List<String> givenNames = providerPatient.getName().stream().map(HumanName::getGiven).flatMap(List::stream).map(
        Object::toString).collect(Collectors.toList());
    Enumerations.AdministrativeGender gender = providerPatient.getGender();
    IQuery<IBaseBundle> matchingQuery = payerFhirClient.search().forResource(Patient.class).where(
        Patient.FAMILY.matches().values(familyNames)).and(Patient.GIVEN.matches().values(givenNames)).and(
        Patient.GENDER.exactly().systemAndCode(gender.getSystem(), gender.toCode())).and(
        Patient.BIRTHDATE.exactly().day(providerPatient.getBirthDate()));

    Bundle results = matchingQuery.returnBundle(Bundle.class).execute();
    if (results.getEntry().isEmpty()) {
      throw new PatientNotFoundException(
          "A patient with the given names '" + String.join(", ", givenNames) + "' and family names '" + String
              .join(", ", familyNames) + "' was not found.");
    }
    if (results.getEntry().size() > 1) {
      throw new PatientNotUniqueException(
          "More than one patient was found with the given names '" + String.join(", ", givenNames)
              + "' and family names '" + String.join(", ", familyNames));
    }
    return (Patient) results.getEntry().iterator().next().getResource();
  }

  private Patient readPatient(IGenericClient fhirClient, String patientId) {
    try {
      return fhirClient.read().resource(Patient.class).withId(patientId).execute();
    } catch (ResourceNotFoundException e) {
      throw new PatientNotFoundException("Patient with id '" + patientId + "' was not found in the system.");
    }
  }

  private IGenericClient createFhirClient(String fhiServerUri, String accessToken) {
    IGenericClient client = fhirContext.newRestfulGenericClient(fhiServerUri);
    if (accessToken != null) {
      IClientInterceptor is = new BearerTokenAuthInterceptor(accessToken);
      client.registerInterceptor(is);
    }
    return client;
  }

  private class SmartAppointmentHook extends CdsService {

    private SmartAppointmentHook() {
      super("smart-appointment-hook", "appointment-book", "Appointment Book",
          "This hook is invoked when the user is scheduling one or more future encounters/visits for the patient.");
    }
  }
}
