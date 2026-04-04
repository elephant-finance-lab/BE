package com.example.elephantfinancelab_be.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecuritySmokeTest {

  @LocalServerPort private int port;

  @Test
  void home_isPublic() throws Exception {
    var res = client().send(get("/"), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, res.statusCode());
  }

  @Test
  void me_returnsUnauthorizedWhenAnonymous() throws Exception {
    var res =
            client()
                    .send(
                            HttpRequest.newBuilder(baseUri("/me")).GET().build(),
                            HttpResponse.BodyHandlers.discarding());

    // 기존 3xx 리다이렉트 체크를 삭제하고, 우리 API 설계인 401 Unauthorized를 체크합니다.
    assertEquals(401, res.statusCode());
  }

  private HttpClient client() {
    // 리다이렉트를 따라가지 않도록 설정 (401 응답을 그대로 받기 위함)
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  }

  private HttpRequest get(String path) {
    return HttpRequest.newBuilder(baseUri(path)).GET().build();
  }

  private URI baseUri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}