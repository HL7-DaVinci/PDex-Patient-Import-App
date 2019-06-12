package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.config;

import ca.uhn.fhir.parser.IParser;
import org.hl7.davinci.pdex.refimpl.importer.Importer;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.davinci.pdex.refimpl.importer.simple.SimpleImporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ImporterConfig {

  @Bean
  public Importer importer(IParser iParser,
      @Value("${payer-b.data-import.exclude-resources}") List<String> excludeResources,
      @Value("${npi.system}") String npiSystem) {
    TargetConfiguration targetConfiguration = new TargetConfiguration(iParser, excludeResources, npiSystem);
    return new SimpleImporter(targetConfiguration);
  }

}
