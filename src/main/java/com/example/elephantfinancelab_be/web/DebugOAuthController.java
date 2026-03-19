package com.example.elephantfinancelab_be.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DebugOAuthController {

  @Value("${spring.security.oauth2.client.registration.google.client-id:}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
  private String clientSecret;

  @GetMapping("/debug/oauth")
  Map<String, Object> oauth() {
    return Map.of(
        "googleClientIdLoaded",
        !clientId.isBlank(),
        "googleClientIdSuffix",
        suffix(clientId),
        "googleClientSecretLoaded",
        !clientSecret.isBlank(),
        "googleClientSecretSuffix",
        suffix(clientSecret));
  }

  private String suffix(String s) {
    if (s == null || s.isBlank()) return "";
    int keep = Math.min(6, s.length());
    return s.substring(s.length() - keep);
  }
}
