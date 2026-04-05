package com.example.elephantfinancelab_be.web;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UiController {

  @GetMapping(value = "/ui", produces = MediaType.TEXT_HTML_VALUE)
  String ui(@AuthenticationPrincipal OidcUser user) {
    String body;
    if (user == null) {
      body =
          """
          <h2>Google 로그인 테스트</h2>
          <p><a href="/oauth2/authorization/google">Google로 로그인</a></p>
          """;
    } else {
      String name = escapeHtml(user.getFullName());
      body =
          """
          <h2>Google 로그인 테스트</h2>
          <p>로그인됨: <b>%s</b></p>
          <p><a href="/me">/me 보기</a></p>
          <form method="post" action="/logout"><button type="submit">로그아웃</button></form>
          """
              .formatted(name);
    }

    return """
        <!doctype html>
        <html lang="ko">
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>ElephantFinance - Login Test</title>
          </head>
          <body style="font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; padding: 24px;">
        %s
          </body>
        </html>
        """
        .formatted(body);
  }

  private static String escapeHtml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
