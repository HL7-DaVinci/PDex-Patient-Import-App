package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.filter;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.web.util.UriComponentsBuilder;

public class LaunchAwareOAuth2ClientContextFilter extends OAuth2ClientContextFilter {

  private RedirectStrategy redirectStrategy;

  public LaunchAwareOAuth2ClientContextFilter() {
    this.redirectStrategy = new DefaultRedirectStrategy();
    setRedirectStrategy(this.redirectStrategy);
  }

  //todo handle doFilter because now it is impacted by previous state

  @Override
  protected void redirectUser(UserRedirectRequiredException e, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String redirectUri = e.getRedirectUri();
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(redirectUri);
    Map<String, String> requestParams = e.getRequestParams();
    for (Map.Entry<String, String> param : requestParams.entrySet()) {
      builder.queryParam(param.getKey(), param.getValue());
    }

    if (e.getStateKey() != null) {
      builder.queryParam("state", e.getStateKey());
    }

    DefaultSavedRequest savedRequest = (DefaultSavedRequest) request.getSession().getAttribute(
        "SPRING_SECURITY_SAVED_REQUEST");

    String launch = getSavedRequestParameter(savedRequest, "launch");
    if (launch != null) {
      builder.queryParam("launch", launch);
    }

    String iss = getSavedRequestParameter(savedRequest, "iss");
    if (launch != null) {
      builder.queryParam("aud", iss);
    }

    redirectStrategy.sendRedirect(request, response, builder.build().encode().toUriString());
  }

  private String getSavedRequestParameter(DefaultSavedRequest savedRequest, String parameterName) {
    String[] parameter = savedRequest.getParameterValues(parameterName);
    String result = null;
    if (parameter != null && parameter.length == 1) {
      result = parameter[0];
    }
    return result;
  }
}
