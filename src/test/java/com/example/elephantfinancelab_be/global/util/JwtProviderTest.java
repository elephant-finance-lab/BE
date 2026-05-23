package com.example.elephantfinancelab_be.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class JwtProviderTest {

  private static final String SECRET =
      "test-secret-key-that-is-long-enough-for-hmac-sha-signing-1234567890";

  private final JwtProvider jwtProvider = new JwtProvider(mock(StringRedisTemplate.class));

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(jwtProvider, "secret", SECRET);
    ReflectionTestUtils.setField(jwtProvider, "refreshExpiration", 60_000L);
  }

  @Test
  void getUserIdAllowExpiredExtractsSubjectFromExpiredSignedToken() {
    ReflectionTestUtils.setField(jwtProvider, "accessExpiration", -1_000L);
    String token = jwtProvider.generateAccessToken("user@example.com");

    assertThat(jwtProvider.validateToken(token)).isFalse();
    assertThat(jwtProvider.getUserIdAllowExpired(token)).contains("user@example.com");
  }

  @Test
  void getUserIdAllowExpiredRejectsTokenWithInvalidSignature() {
    ReflectionTestUtils.setField(jwtProvider, "accessExpiration", 60_000L);
    String token = jwtProvider.generateAccessToken("user@example.com");
    JwtProvider otherProvider = new JwtProvider(mock(StringRedisTemplate.class));
    ReflectionTestUtils.setField(
        otherProvider,
        "secret",
        "different-test-secret-key-that-is-long-enough-for-hmac-sha-signing");

    assertThat(otherProvider.getUserIdAllowExpired(token)).isEmpty();
  }
}
