package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialApiType;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class KisStockFinancialClient {

  private static final String KRX_MARKET_DIV_CODE = "J";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;

  public KisStockFinancialClient(
      KisProperties kisProperties,
      KisAccessTokenClient accessTokenClient,
      ObjectMapper objectMapper,
      WebClient.Builder builder) {
    this.kisProperties = kisProperties;
    this.accessTokenClient = accessTokenClient;
    this.objectMapper = objectMapper;
    this.webClient = builder.baseUrl(normalizedBaseUrl(kisProperties.getBaseUrl())).build();
  }

  public List<JsonNode> fetchFinancial(
      String ticker, StockFinancialApiType apiType, StockFinancialPeriod period) {
    JsonNode root = fetchFinancialRoot(ticker, apiType, period);
    if (!"0".equals(root.path("rt_cd").asText())) {
      log.warn(
          "code={}, message={}, apiType={}, msg_cd={}, msg={}",
          StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getMessage(),
          apiType,
          root.path("msg_cd").asText(),
          root.path("msg1").asText());
      throw new StockException(StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED);
    }

    return toOutputList(root.path("output"));
  }

  private JsonNode fetchFinancialRoot(
      String ticker, StockFinancialApiType apiType, StockFinancialPeriod period) {
    log.debug(
        "한국투자증권 재무제표 API 호출. ticker={}, apiType={}, trId={}, period={}",
        ticker,
        apiType,
        apiType.getTrId(),
        period);
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(apiType.getPath())
                    .queryParam("FID_DIV_CLS_CODE", period.getKisDivClsCode())
                    .queryParam("fid_cond_mrkt_div_code", KRX_MARKET_DIV_CODE)
                    .queryParam("fid_input_iscd", ticker)
                    .build())
        .headers(headers -> applyKisHeaders(headers, apiType.getTrId()))
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> stockFinancialApiException(response.statusCode().value(), body)))
        .bodyToMono(String.class)
        .map(this::readTree)
        .timeout(REQUEST_TIMEOUT)
        .onErrorMap(this::mapStockFinancialException)
        .block();
  }

  private List<JsonNode> toOutputList(JsonNode output) {
    List<JsonNode> items = new ArrayList<>();
    if (!output.isArray()) {
      return items;
    }

    output.forEach(items::add);
    return items;
  }

  private void applyKisHeaders(HttpHeaders headers, String trId) {
    headers.setContentType(
        MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8"));
    headers.setBearerAuth(accessTokenClient.getAccessToken());
    headers.set("appkey", kisProperties.getAppKey());
    headers.set("appsecret", kisProperties.getAppSecret());
    headers.set("tr_id", trId);
    headers.set("custtype", "P");
  }

  private RuntimeException stockFinancialApiException(int statusCode, String body) {
    log.warn(
        "code={}, message={}, status={}, responseBody={}",
        StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getMessage(),
        statusCode,
        body);
    return new StockException(StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED);
  }

  private Throwable mapStockFinancialException(Throwable throwable) {
    if (throwable instanceof StockException) {
      return throwable;
    }
    log.warn(
        "code={}, message={}, exception={}, detail={}",
        StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED.getMessage(),
        throwable.getClass().getSimpleName(),
        throwable.getMessage());
    return new StockException(StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED, throwable);
  }

  private JsonNode readTree(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      log.atWarn()
          .setCause(e)
          .log(
              "code={}, message={}, responseBody={}",
              StockErrorCode.KIS_STOCK_FINANCIAL_RESPONSE_PARSE_FAILED.getCode(),
              StockErrorCode.KIS_STOCK_FINANCIAL_RESPONSE_PARSE_FAILED.getMessage(),
              body);
      throw new StockException(StockErrorCode.KIS_STOCK_FINANCIAL_RESPONSE_PARSE_FAILED, e);
    }
  }

  private String normalizedBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
