package org.hl7.davinci.pdex.refimpl.payer2provider.provider.controllers;

import org.hl7.davinci.pdex.refimpl.payer2provider.provider.dto.CurrentContextDto;
import org.hl7.davinci.pdex.refimpl.payer2provider.provider.dto.CurrentContextResponseDto;
import org.hl7.davinci.pdex.refimpl.payer2provider.provider.service.CallHookService;
import org.hl7.davinci.pdex.refimpl.cdshooks.model.CdsResponse;
import org.hl7.davinci.pdex.refimpl.payer2provider.provider.fhir.FhirResourceNotFoundException;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CallHookController {

  private final CallHookService pdexService;

  @GetMapping("/current-context")
  public CurrentContextResponseDto getCurrentContext(@Valid CurrentContextDto currentContextDto) {
    return pdexService.getCurrentContextDetails(currentContextDto);
  }

  @PostMapping("/call-hook")
  public CdsResponse callHook(@NotEmpty @RequestParam String patientId, @NotEmpty @RequestParam String practitionerId,
      @RequestParam(required = false) String encounterId, @RequestParam(required = false) String coverageId)
      throws FhirResourceNotFoundException {
    return pdexService.callHook(patientId, practitionerId, encounterId, coverageId);
  }

}
