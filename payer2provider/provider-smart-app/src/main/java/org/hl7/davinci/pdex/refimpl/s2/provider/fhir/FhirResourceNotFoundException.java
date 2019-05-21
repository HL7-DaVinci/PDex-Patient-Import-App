package org.hl7.davinci.pdex.refimpl.s2.provider.fhir;

import lombok.Getter;
import org.hl7.fhir.r4.model.Resource;

@Getter
public class FhirResourceNotFoundException extends Exception {

  private final String id;
  private final Class<? extends Resource> resourceClass;

  public FhirResourceNotFoundException(String id, Class<? extends Resource> resourceClass) {
    super(resourceClass.getSimpleName() + " resource with id " + id + " not found.");
    this.id = id;
    this.resourceClass = resourceClass;
  }
}
