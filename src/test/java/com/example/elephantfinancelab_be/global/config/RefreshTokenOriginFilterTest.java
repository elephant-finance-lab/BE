package com.example.elephantfinancelab_be.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenOriginFilterTest {

  private RefreshTokenOriginFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RefreshTokenOriginFilter(new ObjectMapper());
    ReflectionTestUtils.setField(
        filter,
        "corsAllowedOrigins",
        "https://elephantfinancelab.vercel.app,http://elephantfinancelab.kro.kr");
  }

  @Test
  void tokenReissueAllowsConfiguredOrigin() throws Exception {
    MockHttpServletRequest request = tokenReissueRequest();
    request.addHeader("Origin", "https://elephantfinancelab.vercel.app");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(chainCalled).isTrue();
  }

  @Test
  void tokenReissueAllowsConfiguredRefererWhenOriginIsMissing() throws Exception {
    MockHttpServletRequest request = tokenReissueRequest();
    request.addHeader("Referer", "https://elephantfinancelab.vercel.app/chart");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(chainCalled).isTrue();
  }

  @Test
  void tokenReissueRejectsUntrustedOrigin() throws Exception {
    MockHttpServletRequest request = tokenReissueRequest();
    request.addHeader("Origin", "https://evil.example");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(chainCalled).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void tokenReissueRejectsMissingOriginAndReferer() throws Exception {
    MockHttpServletRequest request = tokenReissueRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainCalled = new AtomicBoolean(false);

    filter.doFilter(request, response, chain(chainCalled));

    assertThat(chainCalled).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
  }

  private static MockHttpServletRequest tokenReissueRequest() {
    return new MockHttpServletRequest("POST", "/api/auth/token");
  }

  private static FilterChain chain(AtomicBoolean chainCalled) {
    return (request, response) -> chainCalled.set(true);
  }
}
