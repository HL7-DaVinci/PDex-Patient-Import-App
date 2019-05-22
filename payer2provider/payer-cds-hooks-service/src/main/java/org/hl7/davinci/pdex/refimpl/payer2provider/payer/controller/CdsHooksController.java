package org.hl7.davinci.pdex.refimpl.payer2provider.payer.controller;

import org.hl7.davinci.pdex.refimpl.payer2provider.payer.exception.CdsServiceNotFoundException;
import org.hl7.davinci.pdex.refimpl.payer2provider.payer.exception.PatientNotFoundException;
import org.hl7.davinci.pdex.refimpl.payer2provider.payer.service.CdsHooksService;
import org.hl7.davinci.pdex.refimpl.cdshooks.model.Card;
import org.hl7.davinci.pdex.refimpl.cdshooks.model.CdsRequest;
import org.hl7.davinci.pdex.refimpl.cdshooks.model.CdsResponse;
import org.hl7.davinci.pdex.refimpl.cdshooks.model.CdsServiceInformation;
import org.hl7.davinci.pdex.refimpl.payer2provider.payer.exception.PatientNotUniqueException;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CdsHooksController {

  private final CdsHooksService cdsHooksService;

  @RequestMapping(value = "/cds-services", method = RequestMethod.GET)
  public CdsServiceInformation getCdsServices() {
    return cdsHooksService.getCdsServices();
  }

  @CrossOrigin
  @RequestMapping(value = "/cds-services", method = RequestMethod.OPTIONS)
  public ResponseEntity cdsServicesOptions(HttpServletResponse response) {
    response.setHeader("Allow", "GET, HEAD, POST");
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/cds-services/{id}", method = RequestMethod.OPTIONS)
  public ResponseEntity cdsResponseOptions(@PathVariable String id, HttpServletResponse response) {
    response.setHeader("Allow", "POST");
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/cds-services/{id}", method = RequestMethod.POST)
  public ResponseEntity<CdsResponse> getCdsResponse(@PathVariable String id, @RequestBody CdsRequest cdsRequest) {
    Card card;
    CdsResponse cdsResponse = new CdsResponse();
    try {
      card = cdsHooksService.createCard(id, cdsRequest);
    } catch (CdsServiceNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (PatientNotFoundException e) {
      card = cdsHooksService.createSimpleCard(
          "No patient was found either by subscriber id or by EMR demographics data matching");
      return new ResponseEntity<>(cdsResponse.addCard(card), HttpStatus.NOT_FOUND);
    } catch (PatientNotUniqueException e) {
      card = cdsHooksService.createSimpleCard("More than one record matched patient demographics data from EMR");
      return new ResponseEntity<>(cdsResponse.addCard(card), HttpStatus.UNPROCESSABLE_ENTITY);
    }
    return new ResponseEntity<>(cdsResponse.addCard(card), HttpStatus.OK);
  }

}
