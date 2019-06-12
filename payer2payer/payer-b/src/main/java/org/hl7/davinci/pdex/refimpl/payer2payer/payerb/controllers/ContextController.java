package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.net.URISyntaxException;
import java.util.Optional;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextResponseDto.CoverageResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.Oath2Token;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service.PayerAService;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service.PayerBService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@Validated
@RestController
public class ContextController {

  private final PayerBService payerBService;
  private final PayerAService payerAService;

  private final String payerAFhirServerUrl;

  @Autowired
  public ContextController(
      PayerBService payerBService,
      PayerAService payerAService,
      @Value("${payer-a.fhir-server-uri}") String payerAFhirServerUrl
  ) {
    this.payerBService = payerBService;
    this.payerAService = payerAService;
    this.payerAFhirServerUrl = payerAFhirServerUrl;
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
      session.setAttribute("subscriber-id", coverage.get().getSubscriber());
    } else {
      throw new IllegalArgumentException("Wrong coverage id");
    }
  }

  @GetMapping("/importhistory")
  public RedirectView getHistory(
      @RequestParam("code") String code,
      @RequestParam("state") String state,
      HttpSession session
  ) throws URISyntaxException {
    //tip: don't forget to validate state in real world app
    ResponseEntity<Oath2Token> tokenResponse = payerAService.getPayerAToken(code);
    session.setAttribute("history-token", tokenResponse.getBody());

    return new RedirectView("/?payerServerUrl=" + payerAFhirServerUrl);
  }

}