package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class KisStockBasicInfoClient {

  private static final String BASIC_INFO_PATH =
      "/uapi/domestic-stock/v1/quotations/search-stock-info";
  private static final String BASIC_INFO_TR_ID = "CTPF1002R";
  private static final String DOMESTIC_STOCK_PRODUCT_TYPE = "300";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;

  public KisStockBasicInfoClient(
      KisProperties kisProperties,
      KisAccessTokenClient accessTokenClient,
      ObjectMapper objectMapper,
      WebClient.Builder builder) {
    this.kisProperties = kisProperties;
    this.accessTokenClient = accessTokenClient;
    this.objectMapper = objectMapper;
    this.webClient =
        builder.baseUrl(normalizedBaseUrl(kisProperties.getFinancialBaseUrlOrDefault())).build();
  }

  public String fetchStockName(String ticker) {
    JsonNode root = fetchBasicInfoRoot(ticker);
    if (root == null) {
      log.warn(
          "code={}, message={}, ticker={}, reason=empty-response",
          StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getMessage(),
          ticker);
      throw new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED);
    }
    if (!"0".equals(root.path("rt_cd").asText())) {
      log.warn(
          "code={}, message={}, ticker={}, msg_cd={}, msg={}",
          StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getMessage(),
          ticker,
          root.path("msg_cd").asText(),
          root.path("msg1").asText());
      throw new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED);
    }

    JsonNode output = root.path("output");
    String stockName = textValue(output, "prdt_name");
    if (stockName == null) {
      stockName = textValue(output, "prdt_abrv_name");
    }
    if (stockName == null) {
      log.warn(
          "code={}, message={}, ticker={}, reason=missing-product-name",
          StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED.getMessage(),
          ticker);
      throw new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED);
    }
    return stockName;
  }

  private JsonNode fetchBasicInfoRoot(String ticker) {
    log.debug("한국투자증권 종목 기본정보 API 호출. ticker={}, trId={}", ticker, BASIC_INFO_TR_ID);
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(BASIC_INFO_PATH)
                    .queryParam("PRDT_TYPE_CD", DOMESTIC_STOCK_PRODUCT_TYPE)
                    .queryParam("PDNO", ticker)
                    .build())
        .headers(this::applyKisHeaders)
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> basicInfoApiException(response.statusCode().value(), body)))
        .bodyToMono(String.class)
        .map(this::readTree)
        .timeout(REQUEST_TIMEOUT)
        .onErrorMap(this::mapBasicInfoException)
        .block();
  }

  private void applyKisHeaders(HttpHeaders headers) {
    headers.setContentType(
        MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8"));
    headers.setBearerAuth(
        accessTokenClient.getAccessToken(
            kisProperties.getFinancialAppKeyOrDefault(),
            kisProperties.getFinancialAppSecretOrDefault(),
            kisProperties.getFinancialBaseUrlOrDefault()));
    headers.set("appkey", kisProperties.getFinancialAppKeyOrDefault());
    headers.set("appsecret", kisProperties.getFinancialAppSecretOrDefault());
    headers.set("tr_id", BASIC_INFO_TR_ID);
    headers.set("custtype", "P");
  }

  private RuntimeException basicInfoApiException(int statusCode, String body) {
    log.warn(
        "code={}, message={}, status={}, responseBody={}",
        StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getMessage(),
        statusCode,
        body);
    return new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED);
  }

  private Throwable mapBasicInfoException(Throwable throwable) {
    if (throwable instanceof StockException) {
      return throwable;
    }
    log.warn(
        "code={}, message={}, exception={}, detail={}",
        StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED.getMessage(),
        throwable.getClass().getSimpleName(),
        throwable.getMessage());
    return new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED, throwable);
  }

  private JsonNode readTree(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      log.atWarn()
          .setCause(e)
          .log(
              "code={}, message={}, responseBody={}",
              StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED.getCode(),
              StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED.getMessage(),
              body);
      throw new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_RESPONSE_PARSE_FAILED, e);
    }
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value.trim();
  }

  private String normalizedBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
