package com.healthlx.demo.pdex2019.payer.controller;

import com.healthlx.demo.pdex2019.cdshooks.model.Card;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsRequest;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsResponse;
import com.healthlx.demo.pdex2019.cdshooks.model.CdsServiceInformation;
import com.healthlx.demo.pdex2019.payer.exception.CdsServiceNotFoundException;
import com.healthlx.demo.pdex2019.payer.exception.PatientNotFoundException;
import com.healthlx.demo.pdex2019.payer.exception.PatientNotUniqueException;
import com.healthlx.demo.pdex2019.payer.service.CdsHooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CdsHooksController {

  private final CdsHooksService cdsHooksService;

  @RequestMapping(value = "/cds-services", method = RequestMethod.GET)
  public CdsServiceInformation getCdsServices() {
    return cdsHooksService.getCdsServices();
  }

  @RequestMapping(value = "/cds-services", method = RequestMethod.OPTIONS)
  public ResponseEntity cdsServicesOptions(HttpServletResponse response) {
    response.setHeader("Allow", "GET,HEAD,POST");
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
      return new ResponseEntity<>(cdsResponse.addCard(card), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>(cdsResponse.addCard(card), HttpStatus.OK);
  }

}
