package com.example.elephantfinancelab_be.global.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-expiration}")
  private long accessExpiration;

  @Value("${jwt.refresh-expiration}")
  private long refreshExpiration;

  private final StringRedisTemplate redisTemplate;

  private static final String REFRESH_TOKEN_PREFIX = "RT:";

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String userId) {
    return Jwts.builder()
        .subject(userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessExpiration))
        .signWith(getSigningKey())
        .compact();
  }

  public String generateRefreshToken(String userId) {
    String refreshToken =
        Jwts.builder()
            .subject(userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getSigningKey())
            .compact();

    redisTemplate
        .opsForValue()
        .set(REFRESH_TOKEN_PREFIX + userId, refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);

    return refreshToken;
  }

  public String getUserId(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean validateRefreshToken(String userId, String refreshToken) {
    String stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    return refreshToken != null && refreshToken.equals(stored);
  }

  public void deleteRefreshToken(String userId) {
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
  }
}
