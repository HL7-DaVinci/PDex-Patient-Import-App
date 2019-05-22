package org.hl7.davinci.pdex.refimpl.s2.cdshooks.model;

import lombok.Getter;

@Getter
public class CdsService {

  /**
   * The {id} portion of the URL to this service which is available at {baseUrl}/cds-services/{id}. REQUIRED
   */
  private String id;

  /**
   * The hook this service should be invoked on. REQUIRED
   */
  private String hook;

  /**
   * The human-friendly name of this service. RECOMMENDED
   */
  private String name;

  /**
   * The description of this service. REQUIRED
   */
  private String description;

  public CdsService(String id, String hook, String name, String description) {
    if (id == null) {
      throw new IllegalArgumentException("CDSService id cannot be null.");
    }
    if (hook == null) {
      throw new IllegalArgumentException("CDSService hook cannot be null.");
    }
    if (description == null) {
      throw new IllegalArgumentException("CDSService description cannot be null.");
    }
    this.id = id;
    this.hook = hook;
    this.name = name;
    this.description = description;
  }
}
