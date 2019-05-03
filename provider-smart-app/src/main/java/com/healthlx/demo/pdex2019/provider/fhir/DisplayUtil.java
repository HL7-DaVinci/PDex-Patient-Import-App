package com.healthlx.demo.pdex2019.provider.fhir;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.util.XmlUtil;
import java.util.List;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Resource;

@UtilityClass
public class DisplayUtil {

  public String getDisplay(Resource theResource) {

    StringBuilder b = new StringBuilder();
    if (theResource instanceof IResource) {
      IResource resource = (IResource) theResource;
      List<XMLEvent> xmlEvents = XmlUtil.parse(resource.getText().getDiv().getValue());
      if (xmlEvents != null) {
        for (XMLEvent next : xmlEvents) {
          if (next.isCharacters()) {
            Characters characters = next.asCharacters();
            b.append(characters.getData()).append(" ");
          }
        }
      }
      //IMPORTANT IDomainResource from package org.hl7.fhir.instance.model.api
    } else if (theResource instanceof IDomainResource) {
      IDomainResource resource = (IDomainResource) theResource;
      try {
        String divAsString = resource.getText().getDivAsString();
        List<XMLEvent> xmlEvents = XmlUtil.parse(divAsString);
        if (xmlEvents != null) {
          for (XMLEvent next : xmlEvents) {
            if (next.isCharacters()) {
              Characters characters = next.asCharacters();
              b.append(characters.getData()).append(" ");
            }
          }
        }
      } catch (Exception e) {
        throw new DataFormatException("Unable to convert DIV to string", e);
      }

    }
    return b.toString();
  }
}
