package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisApprovalKeyClient {

  private static final String APPROVAL_PATH = "/oauth2/Approval";

  private final KisProperties kisProperties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public String issueApprovalKey() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(approvalUri())
              .timeout(Duration.ofSeconds(10))
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; utf-8")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody()))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("KIS approval key request failed. status={}", response.statusCode());
        throw new IllegalStateException("KIS approval key request failed");
      }

      JsonNode root = objectMapper.readTree(response.body());
      String approvalKey = root.path("approval_key").asText();
      if (approvalKey == null || approvalKey.isBlank()) {
        throw new IllegalStateException("KIS approval key response does not contain approval_key");
      }

      log.info("KIS approval key issued successfully");
      return approvalKey;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to issue KIS approval key", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while issuing KIS approval key", e);
    }
  }

  private URI approvalUri() {
    String baseUrl = kisProperties.getBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return URI.create(normalizedBaseUrl + APPROVAL_PATH);
  }

  private String requestBody() throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "grant_type",
            "client_credentials",
            "appkey",
            kisProperties.getAppKey(),
            "secretkey",
            kisProperties.getAppSecret()));
  }
}
