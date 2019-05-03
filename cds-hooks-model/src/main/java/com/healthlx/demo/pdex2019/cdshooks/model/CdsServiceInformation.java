package com.healthlx.demo.pdex2019.cdshooks.model;

import java.util.ArrayList;
import java.util.List;

public class CdsServiceInformation {

  private List<CdsService> services;

  /**
   * Add a service.
   *
   * @param servicesItem The service.
   * @return
   */
  public CdsServiceInformation addServicesItem(CdsService servicesItem) {
    if (this.services == null) {
      this.services = new ArrayList<>();
    }
    this.services.add(servicesItem);
    return this;
  }

  public List<CdsService> getServices() {
    return services;
  }

  public void setServices(List<CdsService> services) {
    this.services = services;
  }
}
