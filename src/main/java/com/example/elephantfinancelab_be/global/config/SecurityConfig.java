package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final Environment env;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String[] PUBLIC_URLS = {
          "/", "/ui", "/error",
          "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html",
          "/api/auth/**"
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
                                    .accessDeniedHandler(jsonAccessDeniedHandler));

    http.authorizeHttpRequests(
            auth ->
                    auth.requestMatchers(PUBLIC_URLS).permitAll()
                            .anyRequest().authenticated());

    // 구글, 네이버, 카카오 클라이언트 ID 존재 여부 확인
    String googleClientId = env.getProperty("spring.security.oauth2.client.registration.google.client-id");
    String naverClientId = env.getProperty("spring.security.oauth2.client.registration.naver.client-id");
    String kakaoClientId = env.getProperty("spring.security.oauth2.client.registration.kakao.client-id");

    // 소셜 로그인 설정: 설정파일(.env 등)에 클라이언트 ID가 하나라도 있으면 OAuth2 로그인 활성화
    if ((googleClientId != null && !googleClientId.isBlank()) ||
            (naverClientId != null && !naverClientId.isBlank()) ||
            (kakaoClientId != null && !kakaoClientId.isBlank())) {

      http.oauth2Login(oauth2 -> oauth2
              .defaultSuccessUrl("/main") // 로그인 성공 시 이동할 기본 페이지
      );
    } else {
      http.httpBasic(Customizer.withDefaults());
    }

    // 로그아웃 설정: 기본 로그아웃 기능 활성화
    http.logout(logout -> logout
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
    );

    return http.build();
  }
}