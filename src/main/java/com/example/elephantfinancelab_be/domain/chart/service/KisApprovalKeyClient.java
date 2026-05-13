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
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public String issueApprovalKey() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(approvalUri())
              .timeout(REQUEST_TIMEOUT)
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; utf-8")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody()))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "code={}, message={}, status={}",
            ChartErrorCode.KIS_APPROVAL_KEY_FAILED.getCode(),
            ChartErrorCode.KIS_APPROVAL_KEY_FAILED.getMessage(),
            response.statusCode());
        throw new ChartException(ChartErrorCode.KIS_APPROVAL_KEY_FAILED);
      }

      JsonNode root = objectMapper.readTree(response.body());
      String approvalKey = root.path("approval_key").asText();
      if (approvalKey == null || approvalKey.isBlank()) {
        throw new ChartException(ChartErrorCode.KIS_APPROVAL_KEY_FAILED);
      }

      log.info("한국투자증권 웹소켓 접속키 발급 완료");
      return approvalKey;
    } catch (IOException e) {
      throw new ChartException(ChartErrorCode.KIS_APPROVAL_KEY_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ChartException(ChartErrorCode.KIS_APPROVAL_KEY_FAILED, e);
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
