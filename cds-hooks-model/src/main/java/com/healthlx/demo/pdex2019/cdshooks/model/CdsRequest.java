package com.healthlx.demo.pdex2019.cdshooks.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class CdsRequest {

  @NotNull(message = "unsupported hook")
  private String hook = null;
  @NotNull
  private String hookInstance = null;
  private String fhirServer = null;
  private FhirAuthorization fhirAuthorization = null;
  @NotNull
  private String user = null;
  private String patient = null;
  @NotNull
  private Map<String, Object> context = null;
  private Map<String, Object> prefetch = null;

  public Map<String, Object> getPrefetch() {
    return prefetch;
  }

  public void setPrefetch(Map<String, Object> prefetch) {
    this.prefetch = prefetch;
  }

  public String getHook() {
    return hook;
  }

  public void setHook(String hook) {
    this.hook = hook;
  }

  public String getHookInstance() {
    return hookInstance;
  }

  public void setHookInstance(String hookInstance) {
    this.hookInstance = hookInstance;
  }

  public String getFhirServer() {
    return fhirServer;
  }

  public void setFhirServer(String fhirServer) {
    this.fhirServer = fhirServer;
  }

  public FhirAuthorization getFhirAuthorization() {
    return fhirAuthorization;
  }

  public void setFhirAuthorization(FhirAuthorization oauth) {
    this.fhirAuthorization = oauth;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  @JsonGetter("hookInstance")
  public String getHookInstanceAsString() {
    return hookInstance;
  }

  public String getPatient() {
    return patient;
  }

  public void setPatient(String patient) {
    this.patient = patient;
  }
}
