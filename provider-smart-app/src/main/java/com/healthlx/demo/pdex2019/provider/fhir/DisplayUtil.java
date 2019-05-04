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
    StringBuilder stringBuilder = new StringBuilder();
    if (theResource instanceof IResource) {
      IResource resource = (IResource) theResource;
      parseDiv(stringBuilder, resource.getText().getDiv().getValue());
      //IMPORTANT IDomainResource from package org.hl7.fhir.instance.model.api
    } else if (theResource instanceof IDomainResource) {
      IDomainResource resource = (IDomainResource) theResource;
      try {
        parseDiv(stringBuilder, resource.getText().getDivAsString());
      } catch (Exception e) {
        throw new DataFormatException("Unable to convert DIV to string", e);
      }
    }
    return stringBuilder.toString();
  }

  private void parseDiv(StringBuilder builder, String divAsString) {
    List<XMLEvent> xmlEvents = XmlUtil.parse(divAsString);
    if (xmlEvents != null) {
      for (XMLEvent next : xmlEvents) {
        if (next.isCharacters()) {
          Characters characters = next.asCharacters();
          builder.append(characters.getData()).append(" ");
        }
      }
    }
  }

}
