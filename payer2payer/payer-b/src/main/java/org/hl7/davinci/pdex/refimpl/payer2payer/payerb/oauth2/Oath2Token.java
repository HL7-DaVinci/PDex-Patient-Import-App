package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2;

import lombok.Getter;

@Getter
public class Oath2Token {
  String access_token;
  String token_type;
  String expires_in;
  String refresh_token;
  String scope;
  String id_token;
}
