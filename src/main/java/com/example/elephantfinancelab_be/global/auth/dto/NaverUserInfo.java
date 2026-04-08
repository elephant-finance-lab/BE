package com.example.elephantfinancelab_be.global.auth.dto;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo {
  private Map<String, Object> attributes;

  public NaverUserInfo(Map<String, Object> attributes) {
    Map<String, Object> response = (Map<String, Object>) attributes.get("response");
    this.attributes = (response != null) ? response : Map.of();
  }

  @Override
  public String getProviderId() {
    return (String) attributes.get("id");
  }

  @Override
  public String getProvider() {
    return "naver";
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getName() {
    return (String) attributes.get("name");
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
