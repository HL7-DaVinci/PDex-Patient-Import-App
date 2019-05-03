package com.healthlx.demo.pdex2019.provider.controllers;

import com.healthlx.demo.pdex2019.cdshooks.model.CdsResponse;
import com.healthlx.demo.pdex2019.provider.dto.CurrentContextDto;
import com.healthlx.demo.pdex2019.provider.dto.CurrentContextResponseDto;
import com.healthlx.demo.pdex2019.provider.fhir.FhirResourceNotFoundException;
import com.healthlx.demo.pdex2019.provider.service.CallHookService;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class CallHookController {

  @Autowired
  private CallHookService pdexService;

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
