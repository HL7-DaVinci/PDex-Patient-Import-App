package org.hl7.davinci.pdex.refimpl.importer.simple;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.Importer;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.davinci.pdex.refimpl.importer.mapper.IdentifierMapper;
import org.hl7.davinci.pdex.refimpl.importer.mapper.ImportResourceMapper;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SimpleImporter implements Importer {

  private Logger logger = LoggerFactory.getLogger(SimpleImporter.class);

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
    Patient receivedPatient = (Patient) firstPage.getEntryFirstRep()
        .getResource();

    IdentifierMapper identifierMapper = new IdentifierMapper(importRequest.getTargetClient());
    identifierMapper.importNewIdentifiers(importRequest.getPatientId(), receivedPatient);

    Bundle page = firstPage;
    while (page != null) {
      String message = page.getLink(Bundle.LINK_NEXT) != null ? page.getLink(Bundle.LINK_NEXT)
          .getUrl() : " none";
      logger.info("Page loaded, next = " + message);

      List<Resource> toPersist = new ArrayList<>();
      List<Resource> toRefer = new ArrayList<>();

      for (Bundle.BundleEntryComponent entry : page.getEntry()) {
        Resource resource = entry.getResource();
        if (resource == receivedPatient) {
          continue;
        }
        mapper.mapResourceReferences(resource, importRequest);

        //Set id as null to create a new one on persist.
        resource.setId((String) null);

        if (targetCapability.canPersist(resource)) {
          logger.debug("Adding to bundle " + resource.getClass()
              .getSimpleName());
          toPersist.add(resource);
        } else {
          logger.debug("Adding to document ref bundle  " + resource.getClass()
              .getSimpleName());
          toRefer.add(resource);
        }
      }
      persist(importRequest.getTargetClient(), toPersist);
      refer(importRequest.getTargetClient(), toRefer, receivedPatient);

      page = getNextPage(importRequest.getReceivedClient(), page);
    }
  }

  private void persist(IGenericClient targetClient, List<Resource> toPersist) {
    if (!toPersist.isEmpty()) {
      logger.debug("Persisting bundle of size " + toPersist.size());
      Bundle persistBundle = new Bundle();
      persistBundle.setType(Bundle.BundleType.TRANSACTION);

      toPersist.forEach(resource -> {
        persistBundle.addEntry()
            .setResource(resource)
            .getRequest()
            .setMethod(Bundle.HTTPVerb.POST);
      });

      try {
        targetClient.transaction()
            .withBundle(persistBundle)
            .execute();
      } catch (InvalidRequestException invalidRequestException) {
        logger.error(invalidRequestException.getMessage());
      }
    }
  }

  private void refer(IGenericClient targetClient, List<Resource> toRefer, Patient patient) {
    if (!toRefer.isEmpty()) {
      Bundle documentBundle = new Bundle();
      documentBundle.setType(Bundle.BundleType.TRANSACTION);
      toRefer.forEach(resource -> {
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
      content.setAttachment(new Attachment().setData(targetConfiguration.getParser()
          .encodeResourceToString(documentBundle)
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

    return (Bundle) outParams.getParameterFirstRep()
        .getResource();
  }

  private Bundle getNextPage(IGenericClient payerAClient, Bundle page) {
    Bundle newPage;
    if (page.getLink(Bundle.LINK_NEXT) != null) {
      try {
        newPage = payerAClient.loadPage()
            .next(page)
            .execute();
      } catch (Exception exc) {
        logger.error("Exception occurred when requesting a page from a server. Will stop here. Not all records will be "
            + "imported.", exc);
        newPage = null;
      }
    } else {
      newPage = null;
    }
    return newPage;
  }

  private TargetCapability readTargetCapability(IGenericClient targetClient) {
    CapabilityStatement capabilityStatementB = targetClient.capabilities()
        .ofType(CapabilityStatement.class)
        .execute();

    return new TargetCapability(capabilityStatementB, targetConfiguration.getExcludedResources());
  }
}
