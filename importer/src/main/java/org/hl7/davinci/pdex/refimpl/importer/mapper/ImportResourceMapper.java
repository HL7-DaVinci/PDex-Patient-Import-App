package org.hl7.davinci.pdex.refimpl.importer.mapper;

import org.hl7.davinci.pdex.refimpl.importer.ImportRequest;
import org.hl7.davinci.pdex.refimpl.importer.TargetConfiguration;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class ImportResourceMapper {

  private TargetConfiguration targetConfiguration;

  public ImportResourceMapper(TargetConfiguration targetConfiguration) {
    this.targetConfiguration = targetConfiguration;
  }

  public Resource map(Reference reference, ImportRequest importRequest){
    return new OrganizationMapper(targetConfiguration).readOrCreate(reference, importRequest);
  }

  public Resource map(Resource resource, ImportRequest importRequest){
    return new OrganizationMapper(targetConfiguration).readOrCreate((Organization) resource, importRequest);
  }

}
