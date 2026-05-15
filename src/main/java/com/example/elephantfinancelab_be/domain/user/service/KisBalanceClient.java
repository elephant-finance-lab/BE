package com.example.elephantfinancelab_be.domain.user.service;

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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisBalanceClient {

  private static final String TOKEN_PATH = "/oauth2/tokenP";
  private static final String BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
  private static final String PRODUCT_CODE = "01";

  private final KisProperties kisProperties;
  private final ObjectMapper objectMapper;

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public boolean isValidAccount(String accountNo) {
    try {
      String token = fetchAccessToken();
      String url =
          kisProperties.getBaseUrl()
              + BALANCE_PATH
              + "?CANO="
              + accountNo
              + "&ACNT_PRDT_CD="
              + PRODUCT_CODE
              + "&AFHR_FLPR_YN=N&OFL_YN=&INQR_DVSN=02&UNPR_DVSN=01"
              + "&FUND_STTL_ICLD_YN=N&FNCG_AMT_AUTO_RDPT_YN=N&PRCS_DVSN=01"
              + "&CTX_AREA_FK100=&CTX_AREA_NK100=";

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofSeconds(10))
              .header("Authorization", "Bearer " + token)
              .header("appkey", kisProperties.getAppKey())
              .header("appsecret", kisProperties.getAppSecret())
              .header("tr_id", "VTTC8434R")
              .header("Content-Type", "application/json")
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("KIS balance inquiry failed. status={}", response.statusCode());
        return false;
      }

      JsonNode root = objectMapper.readTree(response.body());
      String rtCd = root.path("rt_cd").asText();
      return "0".equals(rtCd);

    } catch (IOException e) {
      log.warn("KIS balance inquiry IOException", e);
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("KIS balance inquiry interrupted", e);
      return false;
    }
  }

  private String fetchAccessToken() throws IOException, InterruptedException {
    String body =
        objectMapper.writeValueAsString(
            Map.of(
                "grant_type", "client_credentials",
                "appkey", kisProperties.getAppKey(),
                "appsecret", kisProperties.getAppSecret()));

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(kisProperties.getBaseUrl() + TOKEN_PATH))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("KIS token request failed. status=" + response.statusCode());
    }

    JsonNode root = objectMapper.readTree(response.body());
    String token = root.path("access_token").asText();

    if (token == null || token.isBlank()) {
      throw new IllegalStateException("KIS token response does not contain access_token");
    }

    return token;
  }
}
