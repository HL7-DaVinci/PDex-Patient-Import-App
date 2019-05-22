package org.hl7.davinci.pdex.refimpl.s2.cdshooks.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class CdsRequest {

  @NotNull(message = "unsupported hook")
  private String hook;
  @NotNull
  private String hookInstance;
  private String fhirServer;
  private FhirAuthorization fhirAuthorization;
  @NotNull
  private String user;
  private String patient;
  @NotNull
  private Map<String, Object> context;
  private Map<String, Object> prefetch;

  @JsonGetter("hookInstance")
  public String getHookInstanceAsString() {
    return hookInstance;
  }
}
