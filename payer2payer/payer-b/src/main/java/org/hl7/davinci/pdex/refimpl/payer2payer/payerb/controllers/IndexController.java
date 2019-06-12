package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
public class IndexController {

  private String payerAAuthUrl;
  private String payerAClientId;
  private String redirectUri;
  private String aud;

  public IndexController(
          @Value("${payer-a.auth-uri}") String payerAAuthUrl,
          @Value("${payer-a.client-id}") String payerAClientId,
          @Value("${payer-a.redirect-uri}") String redirectUri,
          @Value("${payer-a.fhir-server-uri}") String aud
  ) {
    this.payerAAuthUrl = payerAAuthUrl;
    this.payerAClientId = payerAClientId;
    this.redirectUri = redirectUri;
    this.aud = aud;
  }

  @GetMapping("/launch")
  public String main(
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
