package org.hl7.davinci.pdex.refimpl.importer;

import ca.uhn.fhir.util.ElementUtil;
import com.google.common.collect.ImmutableList;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ImporterTest {

    @Test
    public void testIdUtil() {
        Organization org = (Organization) new Organization().setId("Organization/ORG1");
        Organization org2 = (Organization) new Organization().setId("Organization/ORG2");
        Patient patient = (Patient) new Patient().setId("Patient/1");
        Patient patient2 = (Patient) new Patient().setId("Patient/2");

        Coverage coverage = new Coverage();
        coverage.setPayor(ImmutableList.of( new Reference(org.getId())));
        coverage.setSubscriber(new Reference(patient.getId()));

        Observation obs = new Observation();
        obs.getChildByName()
        List<Reference> references = ElementUtil.allPopulatedChildElements(Reference.class, coverage);
        references.forEach( ref -> {
            if(ref.getType().equals("Patient")){ //and it is ours, else warn or what?
                ref.setId(patient2.getId());
            }
            if(ref.getType().equals("Organization")){ // and lookup for correct one
                ref.setId(org2.getId());
            }
        });

        assertEquals(coverage.getSubscriber().getId(), patient2.getId());
    }
}