package com.example.elephantfinancelab_be.global.security.handler;

import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

  private String extractEmail(OAuth2User oAuth2User) {
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

    return null;
  }

  private String extractProviderUserId(OAuth2User oAuth2User) {
    Map<String, Object> attributes = oAuth2User.getAttributes();

    if (attributes.get("sub") != null) {
      return attributes.get("sub").toString();
    }

    if (attributes.get("response") instanceof Map<?, ?> naverResponse) {
      Object naverId = naverResponse.get("id");
      if (naverId != null) return naverId.toString();
    }

    Object id = attributes.get("id");
    if (id != null) return id.toString();

    return null;
  }

  private String extractName(OAuth2User oAuth2User) {
    Map<String, Object> attributes = oAuth2User.getAttributes();

    if (attributes.get("name") != null) {
      return attributes.get("name").toString();
    }

    if (attributes.get("response") instanceof Map<?, ?> naverResponse) {
      Object naverName = naverResponse.get("name");
      if (naverName != null) return naverName.toString();
    }

    if (attributes.get("kakao_account") instanceof Map<?, ?> kakaoAccount
        && kakaoAccount.get("profile") instanceof Map<?, ?> kakaoProfile) {
      Object kakaoNickname = kakaoProfile.get("nickname");
      if (kakaoNickname != null) return kakaoNickname.toString();
    }

    return "사용자";
  }

  private Provider resolveProvider(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return Provider.valueOf(token.getAuthorizedClientRegistrationId().toUpperCase());
    }

    throw new RuntimeException("소셜 로그인 provider를 찾을 수 없습니다.");
  }

  private User resolveUser(Authentication authentication, OAuth2User oAuth2User) {
    String email = extractEmail(oAuth2User);
    if (email == null || email.isBlank()) {
      throw new RuntimeException("소셜 로그인 사용자 이메일을 찾을 수 없습니다.");
    }

    String providerUserId = extractProviderUserId(oAuth2User);
    if (providerUserId == null || providerUserId.isBlank()) {
      providerUserId = email;
    }

    String finalProviderUserId = providerUserId;
    Optional<User> existingUser =
        userRepository
            .findByEmail(email)
            .or(() -> userRepository.findByProviderUserId(finalProviderUserId));

    return existingUser.orElseGet(
        () ->
            userRepository.save(
                User.builder()
                    .uuid(UUID.randomUUID())
                    .provider(resolveProvider(authentication))
                    .providerUserId(finalProviderUserId)
                    .email(email)
                    .name(extractName(oAuth2User))
                    .active(true)
                    .deleted(false)
                    .build()));
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
            .sameSite("Strict")
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
