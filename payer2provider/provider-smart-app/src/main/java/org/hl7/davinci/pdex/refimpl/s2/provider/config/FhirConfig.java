package org.hl7.davinci.pdex.refimpl.s2.provider.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

  @Bean
  public FhirContext fhirContext() {
    //We are using R4 both at Payer and Provider.
    return FhirContext.forR4();
  }

  @Bean
  public IParser iParser() {
    return fhirContext().newJsonParser().setPrettyPrint(true);
  }

}
