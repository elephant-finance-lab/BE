package com.example.elephantfinancelab_be.global.auth.dto;

import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {
  private Map<String, Object> attributes;
  private Map<String, Object> kakaoAccount;

  public KakaoUserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
    this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
  }

  @Override
  public String getProviderId() {
    return String.valueOf(attributes.get("id"));
  }

  @Override
  public String getProvider() {
    return "kakao";
  }

  @Override
  public String getEmail() {
    if (kakaoAccount == null) return null;
    return (String) kakaoAccount.get("email");
  }

  @Override
  public String getName() {
    if (kakaoAccount == null) return null;
    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
    return (profile != null) ? (String) profile.get("nickname") : null;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
