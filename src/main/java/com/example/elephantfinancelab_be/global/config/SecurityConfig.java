package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String[] SWAGGER_WHITELIST = {
    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html",
  };

  private static void writeApiFailure(HttpServletResponse response, BaseErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    OBJECT_MAPPER.writeValue(response.getOutputStream(), ApiResponse.onFailure(errorCode, null));
  }

  @Bean
  public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
    return (request, response, authException) ->
        writeApiFailure(response, GeneralErrorCode.UNAUTHORIZED);
  }

  @Bean
  public AccessDeniedHandler jsonAccessDeniedHandler() {
    return (request, response, accessDeniedException) ->
        writeApiFailure(response, GeneralErrorCode.FORBIDDEN);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationEntryPoint jsonAuthenticationEntryPoint,
      AccessDeniedHandler jsonAccessDeniedHandler)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(jsonAuthenticationEntryPoint)
                    .accessDeniedHandler(jsonAccessDeniedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SWAGGER_WHITELIST)
                    .permitAll()
                    .requestMatchers("/api/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
