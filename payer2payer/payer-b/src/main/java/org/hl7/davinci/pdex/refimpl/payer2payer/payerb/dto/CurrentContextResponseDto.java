package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.springframework.util.Assert;

@Getter
@Setter
public class CurrentContextResponseDto {

  private PatientResponseDto patient;
  private String encounterId;
  private List<CoverageResponseDto> coverages;

  public CurrentContextResponseDto(Patient patient, Practitioner practitioner, Encounter encounter,
      List<Coverage> coverages) {
    Assert.notNull(patient, "Patient cannot be null");
    //Disabling this check will let us launch and test the App without encounter selection.
    //Assert.notNull(encounter, "Encounter cannot be null");
    Assert.notNull(coverages, "Coverages cannot be null");

    //SET PATIENT
    this.patient = new PatientResponseDto();
    this.patient.setId(patient.getId());
    this.patient.setName(Optional.ofNullable(patient.getNameFirstRep()).map(HumanName::getNameAsSingleString)
        .orElse(null));
    this.patient.setGender(Optional.ofNullable(patient.getGender()).map(AdministrativeGender::getDisplay).orElse(null));
    Optional.ofNullable(patient.getBirthDateElement()).map(DateType::getYear).ifPresent(
        yob -> this.patient.setAge(LocalDate.now().getYear() - yob)
    );

    //SET ENCOUNTER
    this.encounterId = Optional.ofNullable(encounter).map(e -> encounter.getIdElement().getIdPart()).orElse(null);

    //SET COVERAGES - ONLY FIRST PAYOR IS RESOLVED
    this.coverages = coverages.stream().map(coverage -> {
      CoverageResponseDto c = new CoverageResponseDto();
      c.setId(coverage.getId());
      c.setSubscriber(coverage.getSubscriberId());
      IBaseResource payor = Optional.ofNullable(coverage.getPayorFirstRep()).map(Reference::getResource).orElse(null);
      c.setPayorName(Optional.ofNullable(payor).map(res -> {
        String name;
        if (Organization.class.equals(res.getClass())) {
          name = ((Organization) res).getName();
        } else if (Patient.class.equals(res.getClass())) {
          name = Optional.ofNullable(((Patient) res).getNameFirstRep()).map(HumanName::getNameAsSingleString).orElse(
              null);
        } else if (RelatedPerson.class.equals(res.getClass())) {
          name = Optional.ofNullable(((RelatedPerson) res).getNameFirstRep()).map(HumanName::getNameAsSingleString)
              .orElse(null);
        } else {
          name = null;
        }
        return name;
      }).orElse(null));
      return c;
    }).collect(Collectors.toList());
  }

  @Data
  public class PatientResponseDto {

    private String id;
    private String name;
    private String gender;
    private Integer age;
  }

  @Data
  public static class PractitionerResponseDto {

    private String id;
    private String name;
  }

  @Data
  public static class CoverageResponseDto {

    private String id;
    private String subscriber;
    private String payorName;
  }
}
