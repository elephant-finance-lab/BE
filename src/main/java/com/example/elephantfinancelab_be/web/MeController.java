package com.example.elephantfinancelab_be.web;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

  @GetMapping("/me")
  Map<String, Object> me(@AuthenticationPrincipal OidcUser user) {
    return Map.of(
        "sub", user.getSubject(),
        "email", user.getEmail(),
        "name", user.getFullName(),
        "picture", user.getPicture(),
        "claims", user.getClaims());
  }
}
