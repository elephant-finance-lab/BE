package com.example.elephantfinancelab_be.global.auth.service;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.auth.dto.OAuth2UserInfo;
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

  private User saveOrFindUser(OAuth2UserInfo userInfo) {
    String email = userInfo.getRequiredEmail();
    String providerUserId = userInfo.getProviderIdOrEmail();

    return userRepository
        .findByEmail(email)
        .or(() -> userRepository.findByProviderUserId(providerUserId))
        .orElseGet(() -> userRepository.save(userInfo.toUserEntity()));
  }

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    String registrationId = userRequest.getClientRegistration().getRegistrationId();
    OAuth2UserInfo userInfo = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

    saveOrFindUser(userInfo);

    return oAuth2User;
  }
}
