package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.fhir.IGenericClientProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class PayerBService {

  private final IGenericClientProvider clientProvider;

  public PayerBService(
      @Autowired IGenericClientProvider clientProvider
  ) {
    this.clientProvider = clientProvider;
  }


  public CurrentContextResponseDto getCurrentContextDetails(CurrentContextDto currentContext) {
    IGenericClient client = clientProvider.client();
    Patient patient = client.read().resource(Patient.class).withId(currentContext.getPatientId()).execute();


    Encounter encounter = Optional.ofNullable(currentContext.getEncounterId()).map(a -> client.read().resource(
        Encounter.class).withId(a).execute()).orElse(null);

    List<Coverage> coverages = client.search().forResource(Coverage.class).where(
        Coverage.SUBSCRIBER.hasId(patient.getIdElement().getIdPart())).and(Coverage.STATUS.exactly().code("active"))
        .include(Coverage.INCLUDE_PAYOR.asNonRecursive())
        .returnBundle(Bundle.class)
        .execute()
        .getEntry().stream()
        .map(BundleEntryComponent::getResource)
        .filter(Coverage.class::isInstance)
        .map(Coverage.class::cast)
        .collect(Collectors.toList());

    return new CurrentContextResponseDto(patient, null, encounter, coverages);
  }

}
