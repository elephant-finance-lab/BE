package com.example.elephantfinancelab_be.global.auth.dto;

import java.util.Map;

// TODO: User 및 UserRepository 구현 완료 후 DB 저장 로직 추가 예정
public interface OAuth2UserInfo {
  String getProviderId();

  String getProvider();

  String getEmail();

  String getName();

  Map<String, Object> getAttributes();
}
