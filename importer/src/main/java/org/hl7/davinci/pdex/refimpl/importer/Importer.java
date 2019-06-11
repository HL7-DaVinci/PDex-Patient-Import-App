package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public interface Importer {

  void importRecords(ImportRequest importRequest);

}


