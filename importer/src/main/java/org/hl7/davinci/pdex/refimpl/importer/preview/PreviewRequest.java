package org.hl7.davinci.pdex.refimpl.importer.preview;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreviewRequest {

    private final IGenericClient client;
    private final String subscriberId;

}
