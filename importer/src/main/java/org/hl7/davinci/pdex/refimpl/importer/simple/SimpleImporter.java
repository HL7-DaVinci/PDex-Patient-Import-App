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
    public void importRecords(ImportRequest importRequest, IGenericClient targetClient) {

        Parameters outParams = importRequest.getReceivedClient()
                .operation()
                .onInstance(new IdDt(importRequest.getSubscriberId()))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .execute();

        TargetCapability targetCapability = readTargetCapability(targetClient);

        Bundle firstPage = (Bundle) outParams.getParameter()
                .get(0)
                .getResource();
        Patient patient = (Patient) firstPage.getEntry()
                .get(0)
                .getResource();

        Bundle page = firstPage;
        while (page != null) {

            Bundle persistBundle = new Bundle();
            persistBundle.setType(Bundle.BundleType.TRANSACTION);
            Bundle documentBundle = new Bundle();
            persistBundle.setType(Bundle.BundleType.TRANSACTION);

            for (Bundle.BundleEntryComponent bc : page.getEntry()) {

                Resource resource = bc.getResource();
                if (resource == patient) {
                    continue;
                }

                mapResourceReferences(resource, importRequest);

                //Set id as null to create a new one on persist.
                resource.setId((String) null);

                if (targetCapability.canPersist(resource)) {
                    System.out.println("Adding to bundle " + resource.getClass()
                            .getSimpleName());
                    persistBundle.addEntry()
                            .setResource(resource)
                            .getRequest()
                            .setMethod(Bundle.HTTPVerb.POST);
                } else {
                    System.out.println("Adding to document ref bundle  " + resource.getClass()
                            .getSimpleName());
                    documentBundle.addEntry()
                            .setResource(resource)
                            .getRequest()
                            .setMethod(Bundle.HTTPVerb.POST);
                }

            }

            if (!persistBundle.getEntry()
                    .isEmpty()) {
                System.out.println("Persisting bundle of size " + persistBundle.getEntry()
                        .size());

                try {
                    targetClient.transaction()
                            .withBundle(persistBundle)
                            .execute();
                } catch (InvalidRequestException invalidRequestException) {
                    System.out.println(invalidRequestException.getMessage());
                }
            }
            if (!documentBundle.getEntry()
                    .isEmpty()) {
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

            page = getNextBundle(importRequest.getReceivedClient(), page);
        }

    }

    private TargetCapability readTargetCapability(IGenericClient targetClient) {
        CapabilityStatement capabilityStatementB = targetClient
                .capabilities()
                .ofType(CapabilityStatement.class)
                .execute();

        return new TargetCapability(capabilityStatementB, targetConfiguration.getExcludedResources());
    }

    private Bundle getNextBundle(IGenericClient payerAClient, Bundle page) {
    if (page.getLink(Bundle.LINK_NEXT) != null) {
      page = payerAClient.loadPage()
          .next(page)
          .execute();
    } else {
      page = null;
    }
    return page;
  }

  //We map here only simple references, in real use case ALL references should be mapped
  private void mapResourceReferences(Resource resource, ImportRequest importRequest) {
    Reference patientRef = new Reference(importRequest.getPatientId());
    if (resource.getClass() == Procedure.class) {
      ((Procedure) resource).setSubject(patientRef);
    } else if (resource.getClass() == Encounter.class) {
      Encounter encounter = (Encounter) resource;
      encounter.setSubject(patientRef);
      encounter.getParticipant()
          .forEach(loc -> loc.setIndividual(new Reference()));
      encounter.getLocation()
          .forEach(loc -> loc.setLocation(new Reference()));

      encounter.setServiceProvider(new Reference(mapper.map(encounter.getServiceProvider(),importRequest)));
    } else if (resource.getClass() == MedicationDispense.class) {
      ((MedicationDispense) resource).setSubject(patientRef);
    } else if (resource.getClass() == Observation.class) {
      Observation observation = (Observation) resource;
      observation.setSubject(patientRef);
    } else if (resource.getClass() == DocumentReference.class) {
      ((DocumentReference) resource).setSubject(patientRef);
    } else if (resource.getClass() == Coverage.class) {
      Coverage coverage = ((Coverage) resource);
      coverage.setSubscriber(patientRef);
      List<Reference> newReferences = new ArrayList<>();
      for (Reference reference : coverage.getPayor()) {
        newReferences.add(new Reference(mapper.map(reference, importRequest)));
      }
      coverage.setPayor(newReferences);

    } else if (resource.getClass() == Organization.class) {
      mapper.map(resource, importRequest);
    } else if (resource.getClass() == MedicationRequest.class) {
      ((MedicationRequest) resource).setSubject(patientRef);
    } else {
      System.out.println("Mapping references not supported for type" + resource.getClass());
      //throw new NotImplementedException("Setting Patient reference not supported for type " + resource.getClass());
    }
  }
}
