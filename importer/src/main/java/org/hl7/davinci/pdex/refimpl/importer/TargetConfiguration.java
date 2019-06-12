package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.parser.IParser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TargetConfiguration {

  private final IParser parser;
  private final List<String> excludedResources;
  private final String npiSystem;

}
