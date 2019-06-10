package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImportRequest {

  private final IGenericClient receivedClient;
  private final String receivedSystem;
  private final String subscriberId;

  private final String patientId;

}
