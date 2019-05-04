package com.healthlx.demo.pdex2019.provider.controllers;

import com.healthlx.demo.pdex2019.provider.dto.ImportRecordDto;
import com.healthlx.demo.pdex2019.provider.service.DataImportService;

import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DataImportController {

  private final DataImportService dataImportService;

  @GetMapping("/get-payer-records")
  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsFromPayer(@RequestParam String subscriberId,
      @RequestParam String payerServerUrl, String payerServerToken) {
    return dataImportService.getRecordsFromPayer(subscriberId, payerServerUrl, payerServerToken);
  }

  @PostMapping("/import-records")
  public void importRecords(@RequestBody Map<Class<? extends Resource>, Set<String>> importIds,
      @RequestParam String patientId, @RequestParam String payerServerUrl, String payerServerToken) {
    dataImportService.importRecords(importIds, patientId, payerServerUrl, payerServerToken);
  }
}
