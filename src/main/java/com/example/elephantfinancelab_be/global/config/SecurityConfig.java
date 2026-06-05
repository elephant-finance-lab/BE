package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
import com.example.elephantfinancelab_be.global.auth.service.CustomOAuth2UserService;
import com.example.elephantfinancelab_be.global.security.handler.CustomLogoutSuccessHandler;
import com.example.elephantfinancelab_be.global.security.handler.OAuth2LoginSuccessHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final Environment env;
  private final ObjectMapper objectMapper;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final JwtFilter jwtFilter;
  private final RefreshTokenOriginFilter refreshTokenOriginFilter;

  @Value(
      "${app.cors.allowed-origins:https://elephantfinancelab.site,https://www.elephantfinancelab.site}")
  private String corsAllowedOrigins;

  private static final String[] PUBLIC_URLS = {
    "/",
    "/ui",
    "/error",
    "/login/**",
    "/login/oauth2/**",
    "/oauth2/**",
    "/actuator/health",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/api/auth/**",
    "/api/chart/market",
    "/api/chart/ranking",
    "/api/stocks/**",
    "/api/auth/login/**",
    "/ws",
    "/ws/**",
  };

  private void writeApiFailure(HttpServletResponse response, BaseErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), ApiResponse.onFailure(errorCode, null));
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
  public AuthenticationFailureHandler oAuth2LoginFailureHandler() {
    return (request, response, exception) -> {
      String failureRedirectUri =
          env.getProperty("app.oauth2.failure-redirect-uri", "http://localhost:5173/login");
      String redirectUri =
          UriComponentsBuilder.fromUriString(failureRedirectUri)
              .queryParam("error", "oauth_failed")
              .build()
              .toUriString();

      response.sendRedirect(redirectUri);
    };
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.SET_COOKIE));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationEntryPoint jsonAuthenticationEntryPoint,
      AccessDeniedHandler jsonAccessDeniedHandler)
      throws Exception {

    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(jsonAuthenticationEntryPoint)
                    .accessDeniedHandler(jsonAccessDeniedHandler));

    http.authorizeHttpRequests(
        auth -> auth.requestMatchers(PUBLIC_URLS).permitAll().anyRequest().authenticated());

    String googleClientId =
        env.getProperty("spring.security.oauth2.client.registration.google.client-id", "");
    String naverClientId =
        env.getProperty("spring.security.oauth2.client.registration.naver.client-id", "");
    String kakaoClientId =
        env.getProperty("spring.security.oauth2.client.registration.kakao.client-id", "");

    boolean oauthEnabled =
        (googleClientId != null && !googleClientId.isBlank())
            || (naverClientId != null && !naverClientId.isBlank())
            || (kakaoClientId != null && !kakaoClientId.isBlank());

    if (oauthEnabled) {
      http.oauth2Login(
          oauth2 ->
              oauth2
                  .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                  .successHandler(oAuth2LoginSuccessHandler)
                  .failureHandler(oAuth2LoginFailureHandler()));
    } else {
      http.httpBasic(Customizer.withDefaults());
    }

    http.addFilterBefore(refreshTokenOriginFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    http.logout(
        logout ->
            logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"));

    return http.build();
  }
}
