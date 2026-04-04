package com.example.elephantfinancelab_be.global.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @GetMapping("/me")
  public ResponseEntity<?> getMe(@AuthenticationPrincipal OAuth2User oAuth2User) {
    if (oAuth2User == null) {
      return ResponseEntity.status(401).body("로그인 필요");
    }
    return ResponseEntity.ok(oAuth2User.getAttributes());
  }
}
