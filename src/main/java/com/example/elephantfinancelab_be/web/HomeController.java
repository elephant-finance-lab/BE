package com.example.elephantfinancelab_be.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  @GetMapping("/")
  Map<String, String> home() {
    return Map.of("status", "ok", "login", "/oauth2/authorization/google", "me", "/me");
  }
}
