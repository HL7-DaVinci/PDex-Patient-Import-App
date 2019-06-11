package org.hl7.davinci.pdex.refimpl.importer.mapper;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;

public class ImportResourceMapper {

  private OrganizationMapper organizationMapper;
  private PractitionerMapper practitionerMapper;

  public ImportResourceMapper(TargetConfiguration targetConfiguration) {
    organizationMapper = new OrganizationMapper(targetConfiguration);
    practitionerMapper = new PractitionerMapper(targetConfiguration);
  }

  //We mapOrganization only simple references, in real use case ALL references should be mapped
  public void mapResourceReferences(Resource resource, ImportRequest importRequest) {
    Reference patientRef = new Reference(importRequest.getPatientId());
    if (resource.getClass() == Procedure.class) {
      mapProcedure((Procedure) resource, patientRef);
    } else if (resource.getClass() == Encounter.class) {
      mapEncounter((Encounter) resource, importRequest, patientRef);
    } else if (resource.getClass() == MedicationDispense.class) {
      mapMedicationDispense((MedicationDispense) resource, patientRef);
    } else if (resource.getClass() == Observation.class) {
      mapObservation((Observation) resource, patientRef);
    } else if (resource.getClass() == DocumentReference.class) {
      mapDocumentReference((DocumentReference) resource, patientRef);
    } else if (resource.getClass() == Coverage.class) {
      mapCOverage((Coverage) resource, importRequest, patientRef);
    } else if (resource.getClass() == Organization.class) {
      organizationMapper.readOrCreate((Organization) resource, importRequest);
    } else if (resource.getClass() == MedicationRequest.class) {
      ((MedicationRequest) resource).setSubject(patientRef);
    } else {
      System.out.println("Mapping references not supported for type" + resource.getClass());
      //throw new NotImplementedException("Setting Patient reference not supported for type " + resource.getClass());
    }
  }

  private void mapCOverage(Coverage resource, ImportRequest importRequest, Reference patientRef) {
    resource.setSubscriber(patientRef);
    List<Reference> newReferences = new ArrayList<>();
    for (Reference reference : resource.getPayor()) {
      newReferences.add(new Reference(organizationMapper.readOrCreate(reference, importRequest)));
    }
    resource.setPayor(newReferences);
  }

  private void mapDocumentReference(DocumentReference resource, Reference patientRef) {
    resource.setSubject(patientRef);
  }

  private void mapObservation(Observation resource, Reference patientRef) {
    resource.setSubject(patientRef);
  }

  private void mapMedicationDispense(MedicationDispense resource, Reference patientRef) {
    resource.setSubject(patientRef);
  }

  private void mapEncounter(Encounter resource, ImportRequest importRequest, Reference patientRef) {
    resource.setSubject(patientRef);
    resource.getParticipant()
        .forEach(loc ->  {
          Reference individual = loc.getIndividual();
          loc.setIndividual(new Reference(practitionerMapper.readOrCreate(individual,importRequest)));
        });

    resource.getLocation()
        .forEach(loc -> loc.setLocation(new Reference()));

    resource.setServiceProvider(
        new Reference(organizationMapper.readOrCreate(resource.getServiceProvider(), importRequest)));
  }

  private void mapProcedure(Procedure resource, Reference patientRef) {
    resource.setSubject(patientRef);
  }

}
