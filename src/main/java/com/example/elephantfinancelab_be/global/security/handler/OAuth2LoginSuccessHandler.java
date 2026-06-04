package com.example.elephantfinancelab_be.global.security.handler;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.auth.dto.OAuth2UserInfo;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  @Value("${app.oauth2.success-redirect-uri:http://localhost:5173/auth/callback}")
  private String successRedirectUri;

  @Value("${app.auth.refresh-cookie-secure:false}")
  private boolean refreshCookieSecure;

  @Value("${app.auth.refresh-cookie-same-site:Strict}")
  private String refreshCookieSameSite = "Strict";

  private String resolveRegistrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return token.getAuthorizedClientRegistrationId();
    }

    throw new RuntimeException("소셜 로그인 provider를 찾을 수 없습니다.");
  }

  private User resolveUser(Authentication authentication, OAuth2User oAuth2User) {
    OAuth2UserInfo userInfo =
        OAuth2UserInfo.from(resolveRegistrationId(authentication), oAuth2User.getAttributes());
    String email = userInfo.getRequiredEmail();
    String providerUserId = userInfo.getProviderIdOrEmail();

    Optional<User> existingUser =
        userRepository
            .findByEmail(email)
            .or(() -> userRepository.findByProviderUserId(providerUserId));

    return existingUser.orElseGet(() -> userRepository.save(userInfo.toUserEntity()));
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    User user = resolveUser(authentication, oAuth2User);
    String userId = user.getEmail();

    String accessToken = jwtProvider.generateAccessToken(userId);
    String refreshToken = jwtProvider.generateRefreshToken(userId);

    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path("/api/auth/token")
            .maxAge(Duration.ofDays(14))
            .sameSite(refreshCookieSameSite)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    String redirectUri =
        UriComponentsBuilder.fromUriString(successRedirectUri)
            .fragment("token=" + accessToken)
            .build()
            .toUriString();

    response.sendRedirect(redirectUri);
  }
}
