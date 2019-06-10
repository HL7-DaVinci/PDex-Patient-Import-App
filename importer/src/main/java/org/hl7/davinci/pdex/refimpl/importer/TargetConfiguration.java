package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class TargetConfiguration {

  private final IParser parser;
  private final List<String> excludedResources;
  private final String npiSystem;

  @Setter
  IGenericClient client;

  public TargetConfiguration(IParser parser, List<String> excludedResources, String npiSystem) {
    this.parser = parser;
    this.excludedResources = excludedResources;
    this.npiSystem = npiSystem;
  }
}
