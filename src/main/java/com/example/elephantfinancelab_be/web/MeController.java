package com.example.elephantfinancelab_be.web;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User; // 타입 변경
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

  @GetMapping("/me")
  public Map<String, Object> me(@AuthenticationPrincipal OAuth2User user) {
    if (user == null) {
      return Map.of("error", "인증되지 않은 사용자입니다.");
    }

    return user.getAttributes();
  }
}
