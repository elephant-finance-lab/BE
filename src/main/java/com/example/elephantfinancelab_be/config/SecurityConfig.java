package com.example.elephantfinancelab_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
    String googleClientId =
        env.getProperty("spring.security.oauth2.client.registration.google.client-id", "");

    var chain =
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers("/", "/ui", "/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
            .logout(Customizer.withDefaults());

    if (googleClientId != null && !googleClientId.isBlank()) {
      chain.oauth2Login(Customizer.withDefaults());
    } else {
      chain.httpBasic(Customizer.withDefaults());
    }

    return chain.build();
  }
}
