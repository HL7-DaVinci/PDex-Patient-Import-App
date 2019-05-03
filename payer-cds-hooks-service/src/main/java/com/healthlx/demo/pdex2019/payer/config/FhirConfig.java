package com.healthlx.demo.pdex2019.payer.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
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
  public IParser iParser(FhirContext fhirContext) {
    return fhirContext.newJsonParser().setPrettyPrint(true);
  }

  @Bean
  public IGenericClient payerFhirClient(FhirContext fhirContext,
      @Value("${payer.fhir-server-uri}") String payerFhirServer) {
    return fhirContext.newRestfulGenericClient(payerFhirServer);
  }

}
