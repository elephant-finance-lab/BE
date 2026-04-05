package com.example.elephantfinancelab_be.global.auth.dto;

import java.util.Map;

public interface OAuth2UserInfo {
  String getProviderId();

  String getProvider();

  String getEmail();

  String getName();

  Map<String, Object> getAttributes();
}
