package org.hl7.davinci.pdex.refimpl.importer.preview;

import ca.uhn.fhir.model.primitive.IdDt;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ImportPreviewer {

    public Map<Class<? extends Resource>, Set<ImportRecordDto>> getRecordsForImport(
            PreviewRequest previewRequest
    ) {

        Parameters outParams = previewRequest.getClient()
                .operation()
                .onInstance(new IdDt(previewRequest.getSubscriberId()))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .execute();

        Bundle firstPage = (Bundle) outParams.getParameterFirstRep().getResource();
        Patient patient = (Patient) firstPage.getEntryFirstRep().getResource();

        Map<Class<? extends Resource>, Set<ImportRecordDto>> importRecords = new HashMap<>();

        Bundle page = firstPage;
        while (page != null) {

            for (Bundle.BundleEntryComponent bc : page.getEntry()) {
                Resource r = bc.getResource();
                if (r == patient) {
                    continue;
                }
                Set<ImportRecordDto> importRecordDtos = importRecords.computeIfAbsent(r.getClass(), k -> new HashSet<>());
                importRecordDtos.add(new ImportRecordDto(r.getIdElement().getIdPart(), DisplayUtil.getDisplay(r)));
            }

            page = loadNextPage(page, previewRequest);
            System.out.println("Page preloaded, next = " + page.getLink(Bundle.LINK_NEXT) );
        }
        return importRecords;
    }

    private Bundle loadNextPage(Bundle page, PreviewRequest previewRequest) {
        if (page.getLink(Bundle.LINK_NEXT) != null) {
            page = previewRequest.getClient()
                    .loadPage()
                    .next(page)
                    .execute();
        } else {
            page = null;
        }
        return page;
    }

}
