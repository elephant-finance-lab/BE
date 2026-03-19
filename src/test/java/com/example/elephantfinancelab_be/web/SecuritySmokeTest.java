package com.example.elephantfinancelab_be.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void me_redirectsToLoginWhenAnonymous() throws Exception {
    var res =
        client()
            .send(
                HttpRequest.newBuilder(baseUri("/me")).GET().build(),
                HttpResponse.BodyHandlers.discarding());
    assertTrue(res.statusCode() >= 300 && res.statusCode() < 400);
  }

  private HttpClient client() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  }

  private HttpRequest get(String path) {
    return HttpRequest.newBuilder(baseUri(path)).GET().build();
  }

  private URI baseUri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
