package com.example.elephantfinancelab_be.global.security.handler;

import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;

  private String extractUserId(OAuth2User oAuth2User) {
    Map<String, Object> attributes = oAuth2User.getAttributes();

    if (attributes.get("email") != null) {
      return attributes.get("email").toString();
    }

    if (attributes.get("response") instanceof Map<?, ?> naverResponse) {
      Object naverEmail = naverResponse.get("email");
      if (naverEmail != null) return naverEmail.toString();
    }

    if (attributes.get("kakao_account") instanceof Map<?, ?> kakaoAccount) {
      Object kakaoEmail = kakaoAccount.get("email");
      if (kakaoEmail != null) return kakaoEmail.toString();
    }

    Object id = attributes.get("id");
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

    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/api/auth/token")
            .maxAge(Duration.ofDays(14))
            .sameSite("Strict")
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    response.sendRedirect("/api/auth/me#token=" + accessToken);
  }
}
