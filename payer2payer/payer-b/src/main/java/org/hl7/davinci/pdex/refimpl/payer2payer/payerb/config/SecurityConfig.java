package org.hl7.davinci.pdex.refimpl.payer2payer.payerb.config;

import org.hl7.davinci.pdex.refimpl.payer2payer.payerb.oauth2.filter.LaunchAwareOAuth2ClientContextFilter;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;

@Configuration
@EnableOAuth2Sso
// To make sure that Filters created by our WebSecurityConfigurerAdapter take precedence over Filters created by other
// WebSecurityConfigurerAdapters.
@Order(0)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable().headers().frameOptions().disable()
        .and()
        .logout()
        .logoutUrl("/logout") //todo fix better (this ends up in few unsuccessful auth redirects, also this fix is not working if scenario wasn't finished)
        .logoutSuccessUrl("/")
        .deleteCookies("JSESSIONID")
        .permitAll();

    super.configure(http);
  }

  //This requires spring.main.allow-bean-definition-overriding=true in application.properties to override a default
  // filter bean provided by spring-security-oauth2-autoconfigure.
  @Bean
  public OAuth2ClientContextFilter oauth2ClientContextFilter() {
    return new LaunchAwareOAuth2ClientContextFilter();
  }

}
