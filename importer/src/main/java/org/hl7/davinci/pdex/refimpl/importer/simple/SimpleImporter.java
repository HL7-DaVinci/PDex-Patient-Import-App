package org.hl7.davinci.pdex.refimpl.importer.simple;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.Importer;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.davinci.pdex.refimpl.importer.mapper.ImportResourceMapper;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleImporter implements Importer {

    private TargetConfiguration targetConfiguration;
    private ImportResourceMapper mapper;

    public SimpleImporter(TargetConfiguration targetConfiguration) {
        this.targetConfiguration = targetConfiguration;
        mapper = new ImportResourceMapper(targetConfiguration);
    }

    @Override
    public void importRecords(ImportRequest importRequest) {
        Bundle firstPage = readFirstPage(importRequest);
        TargetCapability targetCapability = readTargetCapability(importRequest.getTargetClient());
        Patient patient = (Patient) firstPage.getEntryFirstRep().getResource();

        Bundle page = firstPage;
        while (page != null) {
            List<Resource> toPersist = new ArrayList<>();
            List<Resource> toRefer = new ArrayList<>();

            for (Bundle.BundleEntryComponent entry : page.getEntry()) {
                Resource resource = entry.getResource();
                if (resource == patient) {
                    continue;
                }
                mapper.mapResourceReferences(resource, importRequest);

                //Set id as null to create a new one on persist.
                resource.setId((String) null);

                if (targetCapability.canPersist(resource)) {
                    System.out.println("Adding to bundle " + resource.getClass().getSimpleName());
                    toPersist.add(resource);
                } else {
                    System.out.println("Adding to document ref bundle  " + resource.getClass().getSimpleName());
                    toRefer.add(resource);
                }
            }
            persist(importRequest.getTargetClient(), toPersist);
            refer(importRequest.getTargetClient(), toRefer, patient);

            page = getNextPage(importRequest.getReceivedClient(), page);
        }
    }

    private void persist(IGenericClient targetClient, List<Resource> toPersist) {
        if (!toPersist.isEmpty()) {
            System.out.println("Persisting bundle of size " + toPersist.size());
            Bundle persistBundle = new Bundle();
            persistBundle.setType(Bundle.BundleType.TRANSACTION);

            toPersist.forEach( resource -> {
                persistBundle.addEntry()
                        .setResource(resource)
                        .getRequest()
                        .setMethod(Bundle.HTTPVerb.POST);
            });

            //todo remove temporary logging
            try {
                targetClient.transaction()
                        .withBundle(persistBundle)
                        .execute();
            } catch (InvalidRequestException invalidRequestException) {
                System.out.println(invalidRequestException.getMessage());
            }
        }
    }

    private void refer(IGenericClient targetClient, List<Resource> toRefer, Patient patient) {
        if (!toRefer.isEmpty()) {
            Bundle documentBundle = new Bundle();
            documentBundle.setType(Bundle.BundleType.TRANSACTION);
            toRefer.forEach( resource ->  {
                documentBundle.addEntry()
                        .setResource(resource)
                        .getRequest()
                        .setMethod(Bundle.HTTPVerb.POST);
            });

            DocumentReference documentReference = new DocumentReference();
            documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
            documentReference.setSubject(new Reference(patient));
            DocumentReference.DocumentReferenceContentComponent content =
                    new DocumentReference.DocumentReferenceContentComponent();
            content.setAttachment(new Attachment().setData(targetConfiguration.getParser().encodeResourceToString(documentBundle)
                    .getBytes()));
            documentReference.addContent();
            targetClient.create()
                    .resource(documentReference)
                    .execute();
        }
    }

    private Bundle readFirstPage(ImportRequest importRequest) {
        Parameters outParams = importRequest.getReceivedClient()
                .operation()
                .onInstance(new IdDt(importRequest.getSubscriberId()))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .execute();

        return (Bundle) outParams.getParameterFirstRep().getResource();
    }

    private Bundle getNextPage(IGenericClient payerAClient, Bundle page) {
        if (page.getLink(Bundle.LINK_NEXT) != null) {
            page = payerAClient.loadPage()
                    .next(page)
                    .execute();
        } else {
            page = null;
        }
        return page;
    }

    private TargetCapability readTargetCapability(IGenericClient targetClient) {
        CapabilityStatement capabilityStatementB = targetClient
                .capabilities()
                .ofType(CapabilityStatement.class)
                .execute();

        return new TargetCapability(capabilityStatementB, targetConfiguration.getExcludedResources());
    }
}
