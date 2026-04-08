package com.example.elephantfinancelab_be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "KAKAO_CLIENT_ID=test",
      "KAKAO_CLIENT_SECRET=test",
      "GOOGLE_CLIENT_ID=test",
      "GOOGLE_CLIENT_SECRET=test",
      "NAVER_CLIENT_ID=test",
      "NAVER_CLIENT_SECRET=test",
      "DB_URL=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "DB_USER=sa",
      "DB_PW=",
      "JWT_SECRET=test-secret-key-must-be-at-least-32-characters-long"
    })
class ElephantfinancelabBeApplicationTests {

  @Test
  void contextLoads() {}
}
