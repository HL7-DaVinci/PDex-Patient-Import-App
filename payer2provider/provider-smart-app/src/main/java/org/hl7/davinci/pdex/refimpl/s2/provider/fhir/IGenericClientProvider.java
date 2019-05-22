package org.hl7.davinci.pdex.refimpl.s2.provider.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.davinci.pdex.refimpl.s2.provider.oauth2.context.OAuth2ClientContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IGenericClientProvider {

  private final FhirContext fhirContext;
  private final String fhirServerUri;

  public IGenericClientProvider(@Autowired FhirContext fhirContext,
      @Value("${provider.fhir-server-uri}") String fhirServerUri) {
    this.fhirContext = fhirContext;
    this.fhirServerUri = fhirServerUri;
  }

  public IGenericClient client() {
    return client(fhirServerUri, OAuth2ClientContextHolder.currentContext().getAccessToken().getValue());
  }

  public IGenericClient client(String fhiServerUri, String accessToken) {
    IGenericClient client = fhirContext.newRestfulGenericClient(fhiServerUri);
    if (accessToken != null) {
      IClientInterceptor is = new BearerTokenAuthInterceptor(accessToken);
      client.registerInterceptor(is);
    }
    return client;
  }
}
