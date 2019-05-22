package org.hl7.davinci.pdex.refimpl.payer2provider.provider.config;

import org.hl7.davinci.pdex.refimpl.payer2provider.provider.support.CurrentContextArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new CurrentContextArgumentResolver());
  }
}
