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

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

  private final JwtProvider jwtProvider;

  @Value("${KAKAO_CLIENT_ID}")
  private String kakaoClientId;

  @Value("${app.auth.refresh-cookie-secure:false}")
  private boolean refreshCookieSecure;

  private void revokeRefreshToken(HttpServletRequest request) {
    String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (bearer == null || !bearer.startsWith("Bearer ")) {
      return;
    }

    String accessToken = bearer.substring(7);
    if (!jwtProvider.validateToken(accessToken)) {
      return;
    }

    jwtProvider.deleteRefreshToken(jwtProvider.getUserId(accessToken));
  }

  private void expireRefreshCookie(HttpServletResponse response) {
    ResponseCookie expiredCookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path("/api/auth/token")
            .maxAge(Duration.ZERO)
            .sameSite("Strict")
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
        redirectUrl = "https://nid.naver.com/nidlogin.logout?returl=http://localhost:8080";
      } else if ("kakao".equals(registrationId)) {
        redirectUrl =
            "https://kauth.kakao.com/oauth/logout?client_id="
                + kakaoClientId
                + "&logout_redirect_uri=http://localhost:8080";
      }

      getRedirectStrategy().sendRedirect(request, response, redirectUrl);
      return;
    }

    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
