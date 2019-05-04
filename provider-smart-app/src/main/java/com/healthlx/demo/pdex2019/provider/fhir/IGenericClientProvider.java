package com.healthlx.demo.pdex2019.provider.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import com.healthlx.demo.pdex2019.provider.oauth2.context.OAuth2ClientContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class IGenericClientProvider {

  private final FhirContext fhirContext;

  @Value("${provider.fhir-server-uri}")
  private String fhirServerUri;

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
