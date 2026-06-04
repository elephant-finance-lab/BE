package com.example.elephantfinancelab_be.global.auth.dto;

import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public interface OAuth2UserInfo {
  String getProviderId();

  String getProvider();

  String getEmail();

  String getName();

  Map<String, Object> getAttributes();

  static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
    return switch (registrationId) {
      case "google" -> new GoogleUserInfo(attributes);
      case "naver" -> new NaverUserInfo(attributes);
      case "kakao" -> new KakaoUserInfo(attributes);
      default -> throw new OAuth2AuthenticationException("허용되지 않은 소셜 로그인입니다.");
    };
  }

  default String getRequiredEmail() {
    String email = getEmail();
    if (email == null || email.isBlank()) {
      throw new OAuth2AuthenticationException("이메일 정보를 가져올 수 없습니다.");
    }
    return email;
  }

  default String getProviderIdOrEmail() {
    String providerId = getProviderId();
    return (providerId == null || providerId.isBlank()) ? getRequiredEmail() : providerId;
  }

  default String getNameOrDefault() {
    String name = getName();
    return (name == null || name.isBlank()) ? "사용자" : name;
  }

  default Provider getProviderType() {
    return Provider.valueOf(getProvider().toUpperCase(Locale.ROOT));
  }

  default User toUserEntity() {
    return User.builder()
        .uuid(UUID.randomUUID())
        .provider(getProviderType())
        .providerUserId(getProviderIdOrEmail())
        .email(getRequiredEmail())
        .name(getNameOrDefault())
        .active(true)
        .deleted(false)
        .build();
  }
}
