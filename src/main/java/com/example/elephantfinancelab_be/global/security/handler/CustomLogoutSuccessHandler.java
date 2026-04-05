package com.example.elephantfinancelab_be.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

  @Value("${KAKAO_CLIENT_ID}")
  private String kakaoClientId;

  @Override
  public void onLogoutSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

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

    super.onLogoutSuccess(request, response, authentication);
  }
}
