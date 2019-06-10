package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImportRequest {

  private final String receivedSystem;

  private final IGenericClient client;

  private final String subscriverId;
  private final String patientId;

}
