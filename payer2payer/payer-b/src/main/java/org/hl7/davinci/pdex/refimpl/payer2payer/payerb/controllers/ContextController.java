package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.http.ResponseEntity;
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

  private final String payerFhirServerUrl;

  @Autowired
  public ContextController(PayerBService payerBService, RestTemplate restTemplate,
      @Value("${payer-a.fhir-server-uri}") String payerFhirServerUrl) {
    this.payerBService = payerBService;
    this.restTemplate = restTemplate;
    this.payerFhirServerUrl = payerFhirServerUrl;
  }

  @GetMapping("/current-context")
  public CurrentContextResponseDto getCurrentContext(@Valid CurrentContextDto currentContextDto, HttpSession session) {
    CurrentContextResponseDto currentContextDetails = payerBService.getCurrentContextDetails(currentContextDto);
    if(!currentContextDetails.getCoverages().isEmpty()){
      String subscriberId = currentContextDetails.getCoverages().get(0).getSubscriberId();
      session.setAttribute("subscriber-id", subscriberId);
    }
    session.setAttribute("patient-id", currentContextDetails.getPatient().getId());
    return currentContextDetails;
  }

  @GetMapping("/importhistory")
  public RedirectView getHistory(@RequestParam("code") String code, @RequestParam("state") String state,
      HttpSession session) throws URISyntaxException {
    //todo validate state
    URI uri = new URIBuilder().setScheme("https").setHost("auth.hspconsortium.org").setPath("/token").setParameter(
        "grant_type", "authorization_code").setParameter("redirect_uri", "http://localhost:8080/importhistory")
        .setParameter("client_id", "04c130da-4849-48cf-b29c-a29d60b08b82").setParameter("code", code).build();

    ResponseEntity<Oath2Token> tokenResponse = restTemplate.postForEntity(uri, null, Oath2Token.class);
    session.setAttribute("history-token", tokenResponse.getBody());

    return new RedirectView("/?payerServerUrl=" + payerFhirServerUrl + "&tokenSet=true");
  }

}
