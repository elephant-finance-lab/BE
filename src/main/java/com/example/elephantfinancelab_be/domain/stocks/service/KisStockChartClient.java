package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class KisStockChartClient {

  private static final String TIME_ITEM_CHART_PATH =
      "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice";
  private static final String DAILY_ITEM_CHART_PATH =
      "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
  private static final String TIME_ITEM_CHART_TR_ID = "FHKST03010200";
  private static final String DAILY_ITEM_CHART_TR_ID = "FHKST03010100";
  private static final String KRX_MARKET_DIV_CODE = "J";
  private static final String ADJUSTED_PRICE = "0";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);
  private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);
  private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter KIS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter MINUTE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final WebClient webClient;

  public KisStockChartClient(
      KisProperties kisProperties,
      KisAccessTokenClient accessTokenClient,
      ObjectMapper objectMapper,
      WebClient.Builder builder) {
    this.kisProperties = kisProperties;
    this.accessTokenClient = accessTokenClient;
    this.objectMapper = objectMapper;
    this.webClient = builder.baseUrl(normalizedBaseUrl(kisProperties.getBaseUrl())).build();
  }

  public List<StockChartResDTO.DataPoint> fetchChart(String ticker, StockChartRange range) {
    JsonNode root =
        range == StockChartRange.ONE_DAY
            ? fetchMinuteChart(ticker)
            : fetchPeriodChart(ticker, range);
    if (!"0".equals(root.path("rt_cd").asText())) {
      log.warn(
          "code={}, message={}, msg_cd={}, msg={}",
          StockErrorCode.KIS_STOCK_CHART_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_API_FAILED.getMessage(),
          root.path("msg_cd").asText(),
          root.path("msg1").asText());
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED);
    }

    List<StockChartResDTO.DataPoint> points =
        range == StockChartRange.ONE_DAY
            ? toMinuteDataPoints(root.path("output2"))
            : toPeriodDataPoints(root.path("output2"));
    points.sort(Comparator.comparing(StockChartResDTO.DataPoint::time));
    return points;
  }

  private JsonNode fetchMinuteChart(String ticker) {
    String inputHour = minuteChartInputHour();
    log.debug(
        "한국투자증권 분봉 차트 API 호출. ticker={}, trId={}, inputHour={}",
        ticker,
        TIME_ITEM_CHART_TR_ID,
        inputHour);
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(TIME_ITEM_CHART_PATH)
                    .queryParam("FID_COND_MRKT_DIV_CODE", KRX_MARKET_DIV_CODE)
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_HOUR_1", inputHour)
                    .queryParam("FID_PW_DATA_INCU_YN", "Y")
                    .queryParam("FID_ETC_CLS_CODE", "")
                    .build())
        .headers(headers -> applyKisHeaders(headers, TIME_ITEM_CHART_TR_ID))
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> stockChartApiException(response.statusCode().value(), body)))
        .bodyToMono(String.class)
        .map(this::readTree)
        .timeout(REQUEST_TIMEOUT)
        .onErrorMap(this::mapStockChartException)
        .block();
  }

  private JsonNode fetchPeriodChart(String ticker, StockChartRange range) {
    LocalDate today = LocalDate.now(KOREA_ZONE);
    LocalDate startDate = range.startDate(today);
    log.debug(
        "한국투자증권 기간별 차트 API 호출. ticker={}, trId={}, startDate={}, endDate={}, period={}",
        ticker,
        DAILY_ITEM_CHART_TR_ID,
        startDate.format(KIS_DATE_FORMATTER),
        today.format(KIS_DATE_FORMATTER),
        range.getKisPeriodDivCode());
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path(DAILY_ITEM_CHART_PATH)
                    .queryParam("FID_COND_MRKT_DIV_CODE", KRX_MARKET_DIV_CODE)
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_DATE_1", startDate.format(KIS_DATE_FORMATTER))
                    .queryParam("FID_INPUT_DATE_2", today.format(KIS_DATE_FORMATTER))
                    .queryParam("FID_PERIOD_DIV_CODE", range.getKisPeriodDivCode())
                    .queryParam("FID_ORG_ADJ_PRC", ADJUSTED_PRICE)
                    .build())
        .headers(headers -> applyKisHeaders(headers, DAILY_ITEM_CHART_TR_ID))
        .retrieve()
        .onStatus(
            status -> status.isError(),
            response ->
                response
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> stockChartApiException(response.statusCode().value(), body)))
        .bodyToMono(String.class)
        .map(this::readTree)
        .timeout(REQUEST_TIMEOUT)
        .onErrorMap(this::mapStockChartException)
        .block();
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

  private RuntimeException stockChartApiException(int statusCode, String body) {
    log.warn(
        "code={}, message={}, status={}, responseBody={}",
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getMessage(),
        statusCode,
        body);
    return new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED);
  }

  private Throwable mapStockChartException(Throwable throwable) {
    if (throwable instanceof StockException) {
      return throwable;
    }
    log.warn(
        "code={}, message={}, exception={}, detail={}",
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getMessage(),
        throwable.getClass().getSimpleName(),
        throwable.getMessage());
    return new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED, throwable);
  }

  private JsonNode readTree(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      log.warn(
          "code={}, message={}, responseBody={}",
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getMessage(),
          body,
          e);
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED, e);
    }
  }

  private String minuteChartInputHour() {
    LocalTime now = LocalTime.now(KOREA_ZONE).withNano(0);
    if (now.isBefore(MARKET_OPEN_TIME)) {
      return MARKET_OPEN_TIME.format(KIS_TIME_FORMATTER);
    }
    if (now.isAfter(MARKET_CLOSE_TIME)) {
      return MARKET_CLOSE_TIME.format(KIS_TIME_FORMATTER);
    }
    return now.format(KIS_TIME_FORMATTER);
  }

  private List<StockChartResDTO.DataPoint> toMinuteDataPoints(JsonNode output) {
    List<StockChartResDTO.DataPoint> points = new ArrayList<>();
    if (!output.isArray()) {
      return points;
    }

    for (JsonNode node : output) {
      Long close = positiveLongValue(node, "stck_prpr");
      points.add(
          new StockChartResDTO.DataPoint(
              minuteTime(node),
              close,
              positiveLongValue(node, "stck_oprc"),
              positiveLongValue(node, "stck_hgpr"),
              positiveLongValue(node, "stck_lwpr"),
              close,
              positiveLongValue(node, "cntg_vol")));
    }
    return points;
  }

  private List<StockChartResDTO.DataPoint> toPeriodDataPoints(JsonNode output) {
    List<StockChartResDTO.DataPoint> points = new ArrayList<>();
    if (!output.isArray()) {
      return points;
    }

    for (JsonNode node : output) {
      Long close = positiveLongValue(node, "stck_clpr");
      points.add(
          new StockChartResDTO.DataPoint(
              dateTime(node),
              close,
              positiveLongValue(node, "stck_oprc"),
              positiveLongValue(node, "stck_hgpr"),
              positiveLongValue(node, "stck_lwpr"),
              close,
              positiveLongValue(node, "acml_vol")));
    }
    return points;
  }

  private String minuteTime(JsonNode node) {
    LocalDate date = parseDate(textValue(node, "stck_bsop_date"));
    LocalTime time = parseTime(textValue(node, "stck_cntg_hour"));
    return date.atTime(time.withSecond(0)).format(MINUTE_FORMATTER);
  }

  private String dateTime(JsonNode node) {
    return parseDate(textValue(node, "stck_bsop_date")).format(DATE_FORMATTER);
  }

  private LocalDate parseDate(String value) {
    if (value == null) {
      return LocalDate.now(KOREA_ZONE);
    }

    try {
      return LocalDate.parse(value, KIS_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      log.warn(
          "code={}, message={}, value={}",
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getMessage(),
          value);
      return LocalDate.now(KOREA_ZONE);
    }
  }

  private LocalTime parseTime(String value) {
    if (value == null) {
      return LocalTime.now(KOREA_ZONE).withSecond(0).withNano(0);
    }

    try {
      return LocalTime.parse(value, KIS_TIME_FORMATTER).withSecond(0);
    } catch (DateTimeParseException e) {
      log.warn(
          "code={}, message={}, value={}",
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getMessage(),
          value);
      return LocalTime.now(KOREA_ZONE).withSecond(0).withNano(0);
    }
  }

  private Long positiveLongValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    if (value == null) {
      return 0L;
    }

    try {
      return new BigDecimal(value.trim().replace(",", "")).abs().longValue();
    } catch (NumberFormatException e) {
      log.warn(
          "code={}, message={}, field={}, value={}",
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getMessage(),
          fieldName,
          value);
      return 0L;
    }
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value;
  }

  private String normalizedBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
