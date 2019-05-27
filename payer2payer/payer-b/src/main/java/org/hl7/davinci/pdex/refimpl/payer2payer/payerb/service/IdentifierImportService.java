package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.List;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.fhir.IGenericClientProvider;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class IdentifierImportService {

  private final IGenericClientProvider clientProvider;

  public IdentifierImportService(IGenericClientProvider clientProvider) {this.clientProvider = clientProvider;}

  public void importNewIdentifiers(
      String patientId,
      String subscriberId,
      String payerServerUrl,
      String payerServerToken
  ) {
    IGenericClient bClient = clientProvider.client();
    Patient payerBPatient = bClient.read().resource(Patient.class).withId(patientId).execute();
    List<Identifier> bIdentifiers = payerBPatient.getIdentifier();

    IGenericClient aClient = clientProvider.client(payerServerUrl, payerServerToken);
    Patient payerApatient = aClient.read().resource(Patient.class).withId(subscriberId).execute();

    for (Identifier aIdentifier : payerApatient.getIdentifier()) {
      if (bIdentifiers.stream().noneMatch( identifier -> identifier.getSystem().equals(aIdentifier.getSystem()))) {
        bIdentifiers.add(aIdentifier);
      }
    }

    bClient.update().resource(payerBPatient).execute();
  }

}
