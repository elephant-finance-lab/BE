package com.example.elephantfinancelab_be.global.security.handler;

import com.example.elephantfinancelab_be.global.auth.service.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;

  private String extractUserId(OAuth2User oAuth2User) {
    // 구글
    Object email = oAuth2User.getAttributes().get("email");
    if (email != null) return email.toString();

    // 네이버 - response 안에 중첩
    Object response = oAuth2User.getAttributes().get("response");
    if (response instanceof Map<?, ?> map) {
      Object naverEmail = map.get("email");
      if (naverEmail != null) return naverEmail.toString();
    }

    // 카카오 - id로 fallback
    Object id = oAuth2User.getAttributes().get("id");
    if (id != null) return id.toString();

    throw new RuntimeException("소셜 로그인 사용자 식별자를 찾을 수 없습니다.");
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

    String userId = extractUserId(oAuth2User);

    String accessToken = jwtProvider.generateAccessToken(userId);
    String refreshToken = jwtProvider.generateRefreshToken(userId);

    // Refresh Token → HttpOnly Cookie
    Cookie cookie = new Cookie("refreshToken", refreshToken);
    cookie.setHttpOnly(true);
    cookie.setPath("/api/auth/token");
    cookie.setMaxAge(60 * 60 * 24 * 14);
    response.addCookie(cookie);

    // Access Token → 쿼리파라미터로 프론트에 전달
    response.sendRedirect("/api/auth/me?token=" + accessToken);
  }
}
