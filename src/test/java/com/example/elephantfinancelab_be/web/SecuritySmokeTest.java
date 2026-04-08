package com.example.elephantfinancelab_be.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "KAKAO_CLIENT_ID=test",
      "KAKAO_CLIENT_SECRET=test",
      "GOOGLE_CLIENT_ID=test",
      "GOOGLE_CLIENT_SECRET=test",
      "NAVER_CLIENT_ID=test",
      "NAVER_CLIENT_SECRET=test",
      "DB_URL=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "DB_USER=sa",
      "DB_PW=",
      "JWT_SECRET=test-secret-key-must-be-at-least-32-characters-long"
    })
class SecuritySmokeTest {

  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

  @LocalServerPort private int port;

  @Test
  void home_isPublic() throws Exception {
    var res = client().send(get("/"), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, res.statusCode());
  }

  @Test
  void me_returnsUnauthorizedWhenAnonymous() throws Exception {
    var res = client().send(get("/me"), HttpResponse.BodyHandlers.discarding());
    assertEquals(401, res.statusCode());
  }

  private HttpClient client() {
    return HttpClient.newBuilder()
        .connectTimeout(HTTP_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  private HttpRequest get(String path) {
    return HttpRequest.newBuilder(baseUri(path)).timeout(HTTP_TIMEOUT).GET().build();
  }

  private URI baseUri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
