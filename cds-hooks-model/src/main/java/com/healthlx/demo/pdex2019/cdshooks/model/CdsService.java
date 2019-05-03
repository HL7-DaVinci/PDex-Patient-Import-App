package com.healthlx.demo.pdex2019.cdshooks.model;

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
      throw new NullPointerException("CDSService id cannot be null");
    }
    if (hook == null) {
      throw new NullPointerException("CDSService hook cannot be null");
    }
    if (description == null) {
      throw new NullPointerException("CDSService description cannot be null");
    }
    this.id = id;
    this.hook = hook;
    this.name = name;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getHook() {
    return hook;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}
