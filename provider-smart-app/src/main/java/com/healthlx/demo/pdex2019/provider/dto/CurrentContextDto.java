package com.healthlx.demo.pdex2019.provider.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

@Getter
@Setter
@Data
public class CurrentContextDto {

  @NotBlank
  private final String patientId;
  //Disabling this check will let us launch and test the App without encounter selection.
  //@NotBlank
  private final String encounterId;
  @NotBlank
  private final String userProfile;

  public String getUserType() {
    Assert.notNull(userProfile, "useProfile cannot be null");
    return StringUtils.substringBefore(userProfile, "/");
  }

  public String getUserId() {
    Assert.notNull(userProfile, "useProfile cannot be null");
    return StringUtils.substringAfter(userProfile, "/");
  }
}
