package com.healthlx.demo.pdex2019.provider.oauth2.context;

import javax.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class OAuth2ClientContextHolder {

  public static OAuth2ClientContext currentContext() {
    HttpSession session =
        ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(true);

    DefaultOAuth2ClientContext context = ((DefaultOAuth2ClientContext) session.getAttribute(
        "scopedTarget.oauth2ClientContext"));
    if (context == null) {
      throw new IllegalArgumentException("OAuth2 client context is null.");
    }
    return context;
  }
}
