package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.http.client.utils.URIBuilder;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextResponseDto.CoverageResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.Oath2Token;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service.PayerBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

@Validated
@RestController
public class ContextController {

  private final PayerBService payerBService;
  private final RestTemplate restTemplate;

  private final String payerAFhirServerUrl;
  private final String payerATokenUrl;
  private final String payerAClientId;
  private final String payerARedirectURI;

  @Autowired
  public ContextController(PayerBService payerBService, RestTemplate restTemplate,
      @Value("${payer-a.token-uri}") String payerATokenUrl,
      @Value("${payer-a.fhir-server-uri}") String payerAFhirServerUrl,
      @Value("${payer-a.client-id}") String payerAClientId,
      @Value("${payer-a.redirect-uri}") String payerARedirectURI) {
    this.payerBService = payerBService;
    this.restTemplate = restTemplate;
    this.payerAFhirServerUrl = payerAFhirServerUrl;
    this.payerATokenUrl = payerATokenUrl;
    this.payerAClientId = payerAClientId;
    this.payerARedirectURI = payerARedirectURI;
  }

  @GetMapping("/current-context")
  public CurrentContextResponseDto getCurrentContext(@Valid CurrentContextDto currentContextDto, HttpSession session) {
    CurrentContextResponseDto currentContextDetails = payerBService.getCurrentContextDetails(currentContextDto);
    session.setAttribute("patient-id", currentContextDetails.getPatient().getId());
    return currentContextDetails;
  }

  @GetMapping("/pick-coverage")
  public void selectCoverage(@RequestParam("coverageId") String coverageId, @Valid CurrentContextDto currentContextDto,
      HttpSession session) {
    CurrentContextResponseDto currentContextDetails = payerBService.getCurrentContextDetails(currentContextDto);
    Optional<CoverageResponseDto> coverage = currentContextDetails.getCoverages().stream().filter(
        cov -> cov.getId().equals(coverageId)).findFirst();
    if (coverage.isPresent()) {
      session.setAttribute("subscriber-id", coverage.get().getSubscriberId());
    } else {
      throw new IllegalArgumentException("Wrong coverage id");
    }
  }

  @GetMapping("/importhistory")
  public RedirectView getHistory(@RequestParam("code") String code, @RequestParam("state") String state,
      HttpSession session) throws URISyntaxException {
    //todo validate state
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

    ResponseEntity<Oath2Token> tokenResponse = restTemplate.postForEntity(uri, request, Oath2Token.class);
    session.setAttribute("history-token", tokenResponse.getBody());

    return new RedirectView("/?payerServerUrl=" + payerAFhirServerUrl);
  }

}