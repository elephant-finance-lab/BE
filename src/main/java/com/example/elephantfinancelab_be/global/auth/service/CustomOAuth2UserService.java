package com.example.elephantfinancelab_be.global.auth.service;

import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.auth.dto.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    OAuth2UserInfo userInfo =
        switch (registrationId) {
          case "google" -> new GoogleUserInfo(oAuth2User.getAttributes());
          case "naver" -> new NaverUserInfo(oAuth2User.getAttributes());
          case "kakao" -> new KakaoUserInfo(oAuth2User.getAttributes());
          default -> throw new OAuth2AuthenticationException("허용되지 않은 소셜 로그인입니다.");
        };

    userRepository
        .findByEmail(userInfo.getEmail())
        .orElseGet(
            () ->
                userRepository.save(
                    User.builder()
                        .uuid(UUID.randomUUID())
                        .provider(Provider.valueOf(userInfo.getProvider().toUpperCase()))
                        .providerUserId(userInfo.getProviderId())
                        .email(userInfo.getEmail())
                        .name(userInfo.getName())
                        .active(true)
                        .deleted(false)
                        .build()));

    return oAuth2User;
  }
}
