package com.example.elephantfinancelab_be.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "KAKAO_CLIENT_ID=test",
      "KAKAO_CLIENT_SECRET=test",
      "GOOGLE_CLIENT_ID=test",
      "GOOGLE_CLIENT_SECRET=test",
      "NAVER_CLIENT_ID=test",
      "NAVER_CLIENT_SECRET=test"
    })
class SecuritySmokeTest {

  @Test
  void contextLoads() {}
}
