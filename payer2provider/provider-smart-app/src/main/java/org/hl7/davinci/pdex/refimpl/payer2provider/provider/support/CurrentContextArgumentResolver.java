package org.hl7.davinci.pdex.refimpl.payer2provider.provider.support;

import org.hl7.davinci.pdex.refimpl.payer2provider.provider.dto.CurrentContextDto;
import org.hl7.davinci.pdex.refimpl.payer2provider.provider.oauth2.context.OAuth2ClientContextHolder;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentContextArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterType().equals(CurrentContextDto.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

    OAuth2ClientContext context = OAuth2ClientContextHolder.currentContext();

    String patientId = (String) context.getAccessToken().getAdditionalInformation().get("patient");
    String encounterId = (String) context.getAccessToken().getAdditionalInformation().get("encounter");

    SecurityContext securityContext = SecurityContextHolder.getContext();
    Assert.notNull(securityContext.getAuthentication(), "Authentication object is missing from security context.");

    String profile =
        (String) ((Map) (((OAuth2Authentication) securityContext.getAuthentication()).getUserAuthentication()
            .getDetails())).get("profile");

    CurrentContextDto currentContextDto = new CurrentContextDto(patientId, encounterId, profile);
    if (!"Practitioner".equals(currentContextDto.getUserType())) {
      throw new IllegalStateException("Current user is not a Practitioner");
    }

    return currentContextDto;
  }
}
