package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
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
  private final String payerAFhirServerUrl;
  private final String payerAAuthUrl;
  private final String payerAClientId;

  @Autowired
  public ContextController(
      PayerBService payerBService,
      RestTemplate restTemplate,
      @Value("${payer-a.auth-uri}") String payerAAuthUrl,
      @Value("${payer-a.fhir-server-uri}") String payerAFhirServerUrl,
      @Value("${payer-a.client-id}") String payerAClientId
  ) {
    this.payerBService = payerBService;
    this.restTemplate = restTemplate;
    this.payerAFhirServerUrl = payerAFhirServerUrl;
    this.payerAAuthUrl = payerAAuthUrl;
    this.payerAClientId = payerAClientId;
  }

  @GetMapping("/current-context")
  public CurrentContextResponseDto getCurrentContext(@Valid CurrentContextDto currentContextDto, HttpSession session) {
    CurrentContextResponseDto currentContextDetails = payerBService.getCurrentContextDetails(currentContextDto);
    session.setAttribute("patient-id", currentContextDetails.getPatient().getId());
    return currentContextDetails;
  }

  @GetMapping("/pick-coverage")
  public void selectCoverage(
      @RequestParam("coverageId") String coverageId,
      @Valid CurrentContextDto currentContextDto,
      HttpSession session
  ){
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
      HttpSession session, HttpServletResponse response) throws URISyntaxException {
    //todo validate state
    URI uri = new URIBuilder(payerAAuthUrl)
        .setPath("/token")
        .setParameter("grant_type", "authorization_code")
        .setParameter("redirect_uri", "https://payer-b-smart-app.herokuapp.com/importhistory")
        .setParameter("client_id", payerAClientId)
        .setParameter("code", code)
        .build();

    ResponseEntity<Oath2Token> tokenResponse = restTemplate.postForEntity(uri, null, Oath2Token.class);
    session.setAttribute("history-token", tokenResponse.getBody());

    return new RedirectView("/?payerServerUrl=" + payerAFhirServerUrl);
  }

}