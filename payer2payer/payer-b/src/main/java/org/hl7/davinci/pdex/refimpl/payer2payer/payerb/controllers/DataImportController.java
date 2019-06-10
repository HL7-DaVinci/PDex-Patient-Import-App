package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.Importer;
import org.hl7.davinci.pdex.refimpl.importer.preview.ImportPreviewer;
import org.hl7.davinci.pdex.refimpl.importer.preview.ImportRecordDto;
import org.hl7.davinci.pdex.refimpl.importer.preview.PreviewRequest;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.fhir.IGenericClientProvider;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.Oath2Token;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class DataImportController {

  private final Importer importer;
  private final IGenericClientProvider genericClientProvider;
  private final String receivedSystem;

  @Autowired
  public DataImportController(
          Importer importer,
          IGenericClientProvider genericClientProvider,
          @Value("${payer-a.system}") String receivedSystem
  ) {
    this.importer = importer;
    this.genericClientProvider = genericClientProvider;
    this.receivedSystem = receivedSystem;
  }

  @GetMapping("/get-payer-records")
  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(
          @RequestParam String payerServerUrl,
          HttpSession session
  ) {
    String subscriberId = (String) session.getAttribute("subscriber-id");
    Assert.notNull(subscriberId, "Subscriber Id should not be null. Please select existing coverage");
    Oath2Token payerAToken = (Oath2Token) session.getAttribute("history-token");

    PreviewRequest importRequest = new PreviewRequest(
            genericClientProvider.client(payerServerUrl, payerAToken.getAccess_token()),
            subscriberId
    );
    ImportPreviewer previewer = new ImportPreviewer();
    return previewer.getRecordsForImport(importRequest);

  }

    @PostMapping("/import-records")
    public void importRecords(
            @RequestBody Map<Class<? extends Resource>, Set<String>> importIds,
            @RequestParam String payerServerUrl,
            HttpSession session
    ) {

    Oath2Token payerAToken = (Oath2Token) session.getAttribute("history-token");
    String subscriberId = (String) session.getAttribute("subscriber-id");
    String patientId = (String) session.getAttribute("patient-id");


    ImportRequest importRequest = new ImportRequest(
            genericClientProvider.client(payerServerUrl, payerAToken.getAccess_token()),
            receivedSystem,
            subscriberId,
            patientId);
    importer.importRecords(importRequest, genericClientProvider.client());
  }

}
