package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import ca.uhn.fhir.rest.server.exceptions.UnclassifiedServerFailureException;
import java.util.HashMap;
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
    //We know that server is unstable, so we will try few times :(
    return tryToGetRecordsFromPayerA(
        payerServerUrl,
        subscriberId,
        payerAToken
    );
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

    importService.importRecords(importIds, patientId, payerServerUrl, payerAToken.getAccess_token());
    identifierImportService.importNewIdentifiers(patientId,subscriberId, payerServerUrl, payerAToken.getAccess_token());
  }

  private Map<Class<? extends Resource>, Set<ImportRecordDto>> tryToGetRecordsFromPayerA(
      String payerServerUrl,
      String subscriberId,
      Oath2Token payerAToken
  ) {

    Map<Class<? extends Resource>, Set<ImportRecordDto>> recordsFromPayer = new HashMap<>();

    int attempts = 1;
    while(recordsFromPayer.isEmpty() && attempts < 10) {
      try{
        recordsFromPayer = importService.getRecordsFromPayer(
            subscriberId,
            payerServerUrl,
            payerAToken.getAccess_token()
        );

      }catch (UnclassifiedServerFailureException ex){
        attempts++;
      }
    }
    if(recordsFromPayer.isEmpty()){
      throw new IllegalStateException("Payer A fhir server is not responding");
    }
    return recordsFromPayer;
  }
}
