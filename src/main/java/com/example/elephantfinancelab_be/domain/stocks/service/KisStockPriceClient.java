package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.stocks.converter.StockConverter;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisStockPriceClient {

  private static final String CURRENT_PRICE_PATH =
      "/uapi/domestic-stock/v1/quotations/inquire-price";
  private static final String CURRENT_PRICE_TR_ID = "FHKST01010100";
  private static final String KRX_MARKET_DIV_CODE = "J";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public StockResDTO.Summary fetchSummary(Stock stock) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(currentPriceUri(stock.getTicker()))
              .timeout(REQUEST_TIMEOUT)
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8")
              .header("authorization", "Bearer " + accessTokenClient.getAccessToken())
              .header("appkey", kisProperties.getAppKey())
              .header("appsecret", kisProperties.getAppSecret())
              .header("tr_id", CURRENT_PRICE_TR_ID)
              .header("custtype", "P")
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "{} status={}",
            StockErrorCode.KIS_STOCK_PRICE_API_FAILED.getMessage(),
            response.statusCode());
        throw new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED);
      }

      JsonNode root = objectMapper.readTree(response.body());
      if (!"0".equals(root.path("rt_cd").asText())) {
        log.warn(
            "{} msg_cd={}, msg={}",
            StockErrorCode.KIS_STOCK_PRICE_API_FAILED.getMessage(),
            root.path("msg_cd").asText(),
            root.path("msg1").asText());
        throw new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED);
      }

      JsonNode output = root.path("output");
      String signCode = textValue(output, "prdy_vrss_sign");
      return StockConverter.toSummary(
          stock,
          positiveLongValue(output, "stck_prpr"),
          signedLongValue(output, "prdy_vrss", signCode),
          signedDecimalValue(output, "prdy_ctrt", signCode),
          signCode,
          LocalDateTime.now(KOREA_ZONE).withNano(0));
    } catch (IOException e) {
      throw new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED, e);
    }
  }

  private URI currentPriceUri(String ticker) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("FID_COND_MRKT_DIV_CODE", KRX_MARKET_DIV_CODE);
    params.put("FID_INPUT_ISCD", ticker);

    String baseUrl = kisProperties.getBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return URI.create(normalizedBaseUrl + CURRENT_PRICE_PATH + "?" + queryString(params));
  }

  private String queryString(Map<String, String> params) {
    StringJoiner joiner = new StringJoiner("&");
    params.forEach((key, value) -> joiner.add(encode(key) + "=" + encode(value)));
    return joiner.toString();
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value;
  }

  private Long positiveLongValue(JsonNode node, String fieldName) {
    return decimalValue(node, fieldName).abs().longValue();
  }

  private Long signedLongValue(JsonNode node, String fieldName, String signCode) {
    return applySign(decimalValue(node, fieldName), signCode).longValue();
  }

  private BigDecimal signedDecimalValue(JsonNode node, String fieldName, String signCode) {
    return applySign(decimalValue(node, fieldName), signCode);
  }

  private BigDecimal decimalValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    if (value == null) {
      return BigDecimal.ZERO;
    }

    try {
      return new BigDecimal(value.trim().replace(",", ""));
    } catch (NumberFormatException e) {
      log.warn("한국투자증권 현재가 응답의 숫자 형식이 올바르지 않습니다. field={}, value={}", fieldName, value);
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal applySign(BigDecimal value, String signCode) {
    StockPriceDirection direction = StockPriceDirection.fromSignCode(signCode);
    return switch (direction) {
      case DOWN -> value.abs().negate();
      case FLAT -> BigDecimal.ZERO;
      case UP, UNKNOWN -> value.abs();
    };
  }
}
