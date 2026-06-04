package com.example.elephantfinancelab_be.global.security.handler;

import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

  private final JwtProvider jwtProvider;

  @Value("${KAKAO_CLIENT_ID}")
  private String kakaoClientId;

  @Value("${app.auth.refresh-cookie-secure:false}")
  private boolean refreshCookieSecure;

  @Value("${app.auth.refresh-cookie-same-site:Strict}")
  private String refreshCookieSameSite = "Strict";

  @Value("${app.oauth2.logout-redirect-uri:http://localhost:5173/login}")
  private String logoutRedirectUri;

  private void revokeRefreshToken(HttpServletRequest request) {
    String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (bearer == null || !bearer.startsWith("Bearer ")) {
      return;
    }

    String accessToken = bearer.substring(7);
    jwtProvider.getUserIdAllowExpired(accessToken).ifPresent(jwtProvider::deleteRefreshToken);
  }

  private void expireRefreshCookie(HttpServletResponse response) {
    ResponseCookie expiredCookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path("/api/auth/token")
            .maxAge(Duration.ZERO)
            .sameSite(refreshCookieSameSite)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
  }

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    revokeRefreshToken(request);
    expireRefreshCookie(response);

    String baseUrl =
        request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    String redirectUrl = baseUrl;

    if (authentication instanceof OAuth2AuthenticationToken token) {
      String registrationId = token.getAuthorizedClientRegistrationId();

      if ("naver".equals(registrationId)) {
        redirectUrl =
            UriComponentsBuilder.fromUriString("https://nid.naver.com/nidlogin.logout")
                .queryParam("returl", logoutRedirectUri)
                .build()
                .toUriString();
      } else if ("kakao".equals(registrationId)) {
        redirectUrl =
            UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/logout")
                .queryParam("client_id", kakaoClientId)
                .queryParam("logout_redirect_uri", logoutRedirectUri)
                .build()
                .toUriString();
      }

      getRedirectStrategy().sendRedirect(request, response, redirectUrl);
      return;
    }

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
