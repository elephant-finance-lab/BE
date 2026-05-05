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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final Environment env;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final CustomOAuth2UserService customOAuth2UserService;
  private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final JwtFilter jwtFilter;

  private static final String[] PUBLIC_URLS = {
    "/",
    "/ui",
    "/error",
    "/login/**",
    "/oauth2/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/api/auth/**",
    "/api/chart/market",
    "/api/chart/ranking"
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
  public AuthenticationFailureHandler oAuth2LoginFailureHandler() {
    return (request, response, exception) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      OBJECT_MAPPER.writeValue(
          response.getOutputStream(), ApiResponse.onFailure(GeneralErrorCode.UNAUTHORIZED, null));
    };
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
