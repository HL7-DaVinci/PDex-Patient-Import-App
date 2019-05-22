package org.hl7.davinci.pdex.refimpl.payer2provider.provider.oauth2.context;

import javax.servlet.http.HttpSession;

import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class OAuth2ClientContextHolder {

  private OAuth2ClientContextHolder() {}

  public static OAuth2ClientContext currentContext() {
    HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest()
        .getSession(true);
    DefaultOAuth2ClientContext context = ((DefaultOAuth2ClientContext) session.getAttribute(
        "scopedTarget.oauth2ClientContext"));
    Assert.notNull(context, "OAuth2 client context cannot be null.");
    return context;
  }
}
