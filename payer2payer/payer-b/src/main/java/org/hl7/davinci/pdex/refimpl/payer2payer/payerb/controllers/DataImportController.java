package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.ImportRecordDto;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.Oath2Token;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service.IdentifierImportService;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.service.ImportService;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataImportController {

  private final ImportService importService;
  private final IdentifierImportService identifierImportService;

  @GetMapping("/get-payer-records")
  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(@RequestParam String payerServerUrl, HttpSession session) {
    String subscriberId = (String)session.getAttribute("subscriber-id");
    Assert.notNull(subscriberId, "Subscriber Id should not be null. Please select existing coverage");
    Oath2Token payerAToken = (Oath2Token)session.getAttribute("history-token");
    return importService.getRecordsFromPayer(subscriberId, payerServerUrl, payerAToken.getAccess_token());
  }

  @PostMapping("/import-records")
  public void importRecords(
      @RequestBody Map<Class<? extends Resource>, Set<String>> importIds,
      @RequestParam String payerServerUrl,
      HttpSession session
  ) {

    Oath2Token payerAToken = (Oath2Token)session.getAttribute("history-token");
    String subscriberId = (String)session.getAttribute("subscriber-id");
    String patientId = (String)session.getAttribute("patient-id");

    importService.importRecords(importIds, subscriberId, payerServerUrl, payerAToken.getAccess_token());
    identifierImportService.importNewIdentifiers(patientId,subscriberId, payerServerUrl, payerAToken.getAccess_token());
  }
}
