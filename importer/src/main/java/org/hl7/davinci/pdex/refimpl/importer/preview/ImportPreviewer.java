package org.hl7.davinci.pdex.refimpl.importer.preview;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportPreviewer {

  private Logger logger = LoggerFactory.getLogger(ImportPreviewer.class);

  public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsForImport(PreviewRequest previewRequest) {

    Parameters outParams = previewRequest.getClient()
        .operation()
        .onInstance(new IdDt(previewRequest.getSubscriberId()))
        .named("$everything")
        .withNoParameters(Parameters.class)
        .execute();

    Bundle firstPage = (Bundle) outParams.getParameterFirstRep()
        .getResource();
    Patient patient = (Patient) firstPage.getEntryFirstRep()
        .getResource();

    Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();

    Bundle page = firstPage;
    while (page != null) {
      String message = page.getLink(Bundle.LINK_NEXT) != null ? page.getLink(Bundle.LINK_NEXT)
          .getUrl() : " none";
      logger.info("Page loaded, next = " + message);

      for (Bundle.BundleEntryComponent bc : page.getEntry()) {
        Resource r = bc.getResource();
        if (r == patient) {
          continue;
        }
        Set<ImportRecordDto> importRecordDtos = importRecords.computeIfAbsent(r.getClass(), k -> new HashSet<>());
        importRecordDtos.add(new ImportRecordDto(r.getIdElement()
            .getIdPart(), DisplayUtil.getDisplay(r)));
      }

      page = loadNextPage(page, previewRequest);
    }
    return importRecords;
  }

  private Bundle loadNextPage(Bundle page, PreviewRequest previewRequest) {
    Bundle newPage;
    if (page.getLink(Bundle.LINK_NEXT) != null) {
      try {
        newPage = previewRequest.getClient()
            .loadPage()
            .next(page)
            .execute();
      } catch (Exception exc) {
        logger.error("Exception occurred when requesting a page from a server. Will stop here. Not all records will be "
            + "available in a result set.", exc);
        newPage = null;
      }
    } else {
      newPage = null;
    }
    return newPage;
  }

}
