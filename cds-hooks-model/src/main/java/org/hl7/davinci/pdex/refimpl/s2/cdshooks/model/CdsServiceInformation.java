package org.hl7.davinci.pdex.refimpl.s2.cdshooks.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CdsServiceInformation {

  private List<CdsService> services = new ArrayList<>();

  public CdsServiceInformation addServicesItem(CdsService servicesItem) {
    services.add(servicesItem);
    return this;
  }
}
