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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
  private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) "
              + "else return 0 end",
          Long.class);

  private final KisProperties kisProperties;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final HttpClient httpClient;

  public String getAccessToken() {
    return getAccessToken(
        kisProperties.getAppKey(), kisProperties.getAppSecret(), kisProperties.getBaseUrl());
  }

  public String getAccessToken(String appKey, String appSecret) {
    return getAccessToken(appKey, appSecret, kisProperties.getBaseUrl());
  }

  public String getAccessToken(String appKey, String appSecret, String baseUrl) {
    String tokenRedisKey = tokenRedisKey(appKey, baseUrl);
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
        return issueAccessToken(tokenRedisKey, appKey, appSecret, baseUrl);
      } finally {
        releaseTokenLock(tokenRedisKey, lockValue);
      }
    }

    return waitForIssuedToken(tokenRedisKey);
  }

  public void invalidateAccessToken() {
    invalidateAccessToken(kisProperties.getAppKey(), kisProperties.getBaseUrl());
  }

  public void invalidateAccessToken(String appKey, String baseUrl) {
    String tokenRedisKey = tokenRedisKey(appKey, baseUrl);
    stringRedisTemplate.delete(tokenRedisKey);
    stringRedisTemplate.delete(tokenLockRedisKey(tokenRedisKey));
    log.info(
        "한국투자증권 접근 토큰 캐시 무효화. baseUrl={}, appKey={}",
        normalizedBaseUrl(baseUrl),
        maskedAppKey(appKey));
  }

  private String issueAccessToken(
      String tokenRedisKey, String appKey, String appSecret, String baseUrl) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(tokenUri(baseUrl))
              .timeout(REQUEST_TIMEOUT)
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody(appKey, appSecret)))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "code={}, message={}, status={}, baseUrl={}, appKey={}, responseBody={}",
            ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getCode(),
            ChartErrorCode.KIS_ACCESS_TOKEN_FAILED.getMessage(),
            response.statusCode(),
            normalizedBaseUrl(baseUrl),
            maskedAppKey(appKey),
            response.body());
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
    stringRedisTemplate.execute(
        RELEASE_LOCK_SCRIPT, List.of(tokenLockRedisKey(tokenRedisKey)), lockValue);
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

  private URI tokenUri(String baseUrl) {
    return URI.create(normalizedBaseUrl(baseUrl) + TOKEN_PATH);
  }

  private String requestBody(String appKey, String appSecret) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of("grant_type", "client_credentials", "appkey", appKey, "appsecret", appSecret));
  }

  private String tokenRedisKey(String appKey, String baseUrl) {
    return TOKEN_REDIS_KEY_PREFIX + keyFingerprint(appKey, baseUrl);
  }

  private String tokenLockRedisKey(String tokenRedisKey) {
    return tokenRedisKey + TOKEN_LOCK_REDIS_KEY_SUFFIX;
  }

  private String keyFingerprint(String appKey, String baseUrl) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash =
          digest.digest(
              (normalizedBaseUrl(baseUrl) + ":" + appKey).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      throw new ChartException(ChartErrorCode.KIS_ACCESS_TOKEN_FAILED, e);
    }
  }

  private String normalizedBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  private String maskedAppKey(String appKey) {
    if (appKey == null || appKey.isBlank()) {
      return "blank";
    }
    if (appKey.length() <= 8) {
      return "****";
    }
    return appKey.substring(0, 4) + "****" + appKey.substring(appKey.length() - 4);
  }
}
