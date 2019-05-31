package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import java.util.UUID;
import javax.validation.Valid;
import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.dto.CurrentContextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IndexController {

  @Value("${payer-a.auth-uri}")
  String payerAAuthUrl;

  @Value("${payer-a.client-id}")
  String payerAClientId;

  @Value("${payer-a.redirect-uri}")
  String redirectUri;

  @Value("${payer-a.fhir-server-uri}")
  String aud;

  @GetMapping("/")
  public String main(
      @RequestParam(required = false) String iss,
      @RequestParam(required = false) String launch,//todo clear our session here when oath2 issue resolved
      Model model
  ) {
    model.addAttribute("payerAAuthUrl",payerAAuthUrl);
    model.addAttribute("payerAClientId", payerAClientId);
    model.addAttribute("redirectUri",redirectUri);
    model.addAttribute("state", UUID.randomUUID().toString());
    model.addAttribute("aud",aud);
    return "index";
  }

}
