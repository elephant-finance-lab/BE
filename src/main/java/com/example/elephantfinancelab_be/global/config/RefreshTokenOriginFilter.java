package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RefreshTokenOriginFilter extends OncePerRequestFilter {

  private static final String TOKEN_REISSUE_PATH = "/api/auth/token";

  private final ObjectMapper objectMapper;

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String corsAllowedOrigins;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!requiresOriginCheck(request) || isAllowedBrowserRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(GeneralErrorCode.FORBIDDEN.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(
        response.getOutputStream(), ApiResponse.onFailure(GeneralErrorCode.FORBIDDEN, null));
  }

  private boolean requiresOriginCheck(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod())
        && TOKEN_REISSUE_PATH.equals(requestPath(request));
  }

  private boolean isAllowedBrowserRequest(HttpServletRequest request) {
    String origin = request.getHeader("Origin");
    if (StringUtils.hasText(origin)) {
      return allowedOrigins().contains(normalizeOrigin(origin).orElse(""));
    }

    String referer = request.getHeader("Referer");
    if (StringUtils.hasText(referer)) {
      return allowedOrigins().contains(normalizeOrigin(referer).orElse(""));
    }

    return false;
  }

  private Set<String> allowedOrigins() {
    return Arrays.stream(corsAllowedOrigins.split(","))
        .map(this::normalizeOrigin)
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableSet());
  }

  private Optional<String> normalizeOrigin(String value) {
    try {
      URI uri = URI.create(value.trim());
      if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
        return Optional.empty();
      }
      String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
      return Optional.of(
          uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase() + port);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static String requestPath(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String requestUri = request.getRequestURI();
    if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
      return requestUri.substring(contextPath.length());
    }
    return requestUri;
  }
}
