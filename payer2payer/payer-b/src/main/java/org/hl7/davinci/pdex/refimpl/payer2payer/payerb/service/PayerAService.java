package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.Oath2Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Service
public class PayerAService {

  private String payerATokenUrl;
  private String payerARedirectURI;
  private String payerAClientId;
  private RestTemplate restTemplate;

  public PayerAService(
      @Value("${payer-a.token-uri}") String payerATokenUrl,
      @Value("${payer-a.client-id}") String payerAClientId,
      @Value("${payer-a.redirect-uri}") String payerARedirectURI,
      RestTemplate restTemplate
  ) {
    this.payerATokenUrl = payerATokenUrl;
    this.payerARedirectURI = payerARedirectURI;
    this.payerAClientId = payerAClientId;
    this.restTemplate = restTemplate;
  }

  public ResponseEntity<Oath2Token> getPayerAToken(@RequestParam("code") String code) throws URISyntaxException {
    URI uri = new URIBuilder(payerATokenUrl).build();

    //Set Form Data
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("grant_type", "authorization_code");
    map.add("redirect_uri", payerARedirectURI);
    map.add("client_id", payerAClientId);
    map.add("code", code);
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

    return restTemplate.postForEntity(uri, request, Oath2Token.class);
  }

}
