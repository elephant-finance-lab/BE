package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.exception.ChartException;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisAccessTokenClient {

  private static final String TOKEN_PATH = "/oauth2/tokenP";
  private static final String TOKEN_REDIS_KEY_PREFIX = "kis:access-token:";
  private static final String TOKEN_LOCK_REDIS_KEY_SUFFIX = ":lock";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration TOKEN_LOCK_TTL = Duration.ofSeconds(15);
  private static final Duration TOKEN_WAIT_TIMEOUT = Duration.ofSeconds(8);
  private static final Duration TOKEN_WAIT_INTERVAL = Duration.ofMillis(100);

  private final KisProperties kisProperties;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final HttpClient httpClient;

  public String getAccessToken() {
    String tokenRedisKey = tokenRedisKey();
    String cachedToken = stringRedisTemplate.opsForValue().get(tokenRedisKey);
    if (cachedToken != null && !cachedToken.isBlank()) {
      return cachedToken;
    }

    String lockValue = UUID.randomUUID().toString();
    if (acquireTokenLock(tokenRedisKey, lockValue)) {
      try {
        String tokenAfterLock = stringRedisTemplate.opsForValue().get(tokenRedisKey);
        if (tokenAfterLock != null && !tokenAfterLock.isBlank()) {
          return tokenAfterLock;
        }
        return issueAccessToken(tokenRedisKey);
      } finally {
        releaseTokenLock(tokenRedisKey, lockValue);
      }
    }

    return waitForIssuedToken(tokenRedisKey);
  }

  private String issueAccessToken(String tokenRedisKey) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(tokenUri())
              .timeout(REQUEST_TIMEOUT)
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody()))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "code={}, message={}, status={}",
            ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getCode(),
            ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getMessage(),
            response.statusCode());
        throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED);
      }

      JsonNode root = objectMapper.readTree(response.body());
      String accessToken = root.path("access_token").asText();
      if (accessToken == null || accessToken.isBlank()) {
        throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED);
      }

      long expiresIn = root.path("expires_in").asLong(86_400);
      Duration ttl = Duration.ofSeconds(Math.max(60, expiresIn - 60));
      stringRedisTemplate.opsForValue().set(tokenRedisKey, accessToken, ttl);
      log.info("한국투자증권 접근 토큰 발급 완료");
      return accessToken;
    } catch (IOException e) {
      throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED, e);
    }
  }

  private boolean acquireTokenLock(String tokenRedisKey, String lockValue) {
    Boolean locked =
        stringRedisTemplate
            .opsForValue()
            .setIfAbsent(tokenLockRedisKey(tokenRedisKey), lockValue, TOKEN_LOCK_TTL);
    return Boolean.TRUE.equals(locked);
  }

  private void releaseTokenLock(String tokenRedisKey, String lockValue) {
    String currentLockValue =
        stringRedisTemplate.opsForValue().get(tokenLockRedisKey(tokenRedisKey));
    if (lockValue.equals(currentLockValue)) {
      stringRedisTemplate.delete(tokenLockRedisKey(tokenRedisKey));
    }
  }

  private String waitForIssuedToken(String tokenRedisKey) {
    long deadline = System.nanoTime() + TOKEN_WAIT_TIMEOUT.toNanos();
    while (System.nanoTime() < deadline) {
      String token = stringRedisTemplate.opsForValue().get(tokenRedisKey);
      if (token != null && !token.isBlank()) {
        return token;
      }

      try {
        Thread.sleep(TOKEN_WAIT_INTERVAL.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED, e);
      }
    }

    log.warn(
        "code={}, message={}, reason=wait-timeout",
        ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getCode(),
        ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getMessage());
    throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED);
  }

  private URI tokenUri() {
    return URI.create(normalizedBaseUrl() + TOKEN_PATH);
  }

  private String requestBody() throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "grant_type",
            "client_credentials",
            "appkey",
            kisProperties.getAppKey(),
            "appsecret",
            kisProperties.getAppSecret()));
  }

  private String tokenRedisKey() {
    return TOKEN_REDIS_KEY_PREFIX + keyFingerprint();
  }

  private String tokenLockRedisKey(String tokenRedisKey) {
    return tokenRedisKey + TOKEN_LOCK_REDIS_KEY_SUFFIX;
  }

  private String keyFingerprint() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash =
          digest.digest(
              (normalizedBaseUrl() + ":" + kisProperties.getAppKey())
                  .getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED, e);
    }
  }

  private String normalizedBaseUrl() {
    String baseUrl = kisProperties.getBaseUrl();
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
