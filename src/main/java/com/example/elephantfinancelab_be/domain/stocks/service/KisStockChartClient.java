package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockDailyPriceResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private static final String CURRENT_DAILY_PRICE_PATH =
      "/uapi/domestic-stock/v1/quotations/inquire-daily-price";
  private static final String TIME_ITEM_CHART_TR_ID = "FHKST03010200";
  private static final String DAILY_ITEM_CHART_TR_ID = "FHKST03010100";
  private static final String CURRENT_DAILY_PRICE_TR_ID = "FHKST01010400";
  private static final String KRX_MARKET_DIV_CODE = "J";
  private static final String DAILY_PERIOD_DIV_CODE = "D";
  private static final String ADJUSTED_PRICE = "0";
  private static final int ONE_WEEK_DISPLAY_POINT_COUNT = 7;
  private static final int MINUTE_CHART_MAX_REQUEST_COUNT = 16;
  private static final Duration MINUTE_CHART_PAGE_INTERVAL = Duration.ofMillis(400);
  private static final Duration MINUTE_CHART_RETRY_BACKOFF = Duration.ofMillis(1200);
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
    this.webClient =
        builder.baseUrl(normalizedBaseUrl(kisProperties.getFinancialBaseUrlOrDefault())).build();
  }

  public List<StockChartResDTO.DataPoint> fetchChart(String ticker, StockChartRange range) {
    if (range == StockChartRange.ONE_DAY) {
      List<StockChartResDTO.DataPoint> points = fetchMinuteChartPoints(ticker);
      points.sort(Comparator.comparing(StockChartResDTO.DataPoint::time));
      return points;
    }

    JsonNode root = fetchPeriodChart(ticker, range);
    if (!"0".equals(root.path("rt_cd").asText())) {
      log.warn(
          "code={}, message={}, msg_cd={}, msg={}",
          StockErrorCode.KIS_STOCK_CHART_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_CHART_API_FAILED.getMessage(),
          root.path("msg_cd").asText(),
          root.path("msg1").asText());
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED);
    }

    List<StockChartResDTO.DataPoint> points = toPeriodDataPoints(root.path("output2"));
    points.sort(Comparator.comparing(StockChartResDTO.DataPoint::time));
    return displayPoints(points, range);
  }

  public List<StockDailyPriceResDTO.Item> fetchDailyPrices(String ticker) {
    JsonNode root = fetchCurrentDailyPrice(ticker);
    if (!"0".equals(root.path("rt_cd").asText())) {
      log.warn(
          "code={}, message={}, msg_cd={}, msg={}",
          StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getMessage(),
          root.path("msg_cd").asText(),
          root.path("msg1").asText());
      throw new StockException(StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED);
    }

    List<StockDailyPriceResDTO.Item> items = toDailyPriceItems(root.path("output"));
    items.sort(Comparator.comparing(StockDailyPriceResDTO.Item::date).reversed());
    return items;
  }

  private JsonNode fetchMinuteChart(String ticker, String inputHour, boolean includePastData) {
    log.debug(
        "한국투자증권 분봉 차트 API 호출. ticker={}, trId={}, inputHour={}, includePastData={}",
        ticker,
        TIME_ITEM_CHART_TR_ID,
        inputHour,
        includePastData);
    JsonNode root =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(TIME_ITEM_CHART_PATH)
                        .queryParam("FID_COND_MRKT_DIV_CODE", KRX_MARKET_DIV_CODE)
                        .queryParam("FID_INPUT_ISCD", ticker)
                        .queryParam("FID_INPUT_HOUR_1", inputHour)
                        .queryParam("FID_PW_DATA_INCU_YN", includePastData ? "Y" : "N")
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
    return requireKisResponse(root, StockErrorCode.KIS_STOCK_CHART_API_FAILED);
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
    JsonNode root =
        webClient
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
    return requireKisResponse(root, StockErrorCode.KIS_STOCK_CHART_API_FAILED);
  }

  private JsonNode fetchCurrentDailyPrice(String ticker) {
    log.debug(
        "한국투자증권 일별 시세 API 호출. ticker={}, trId={}, period={}",
        ticker,
        CURRENT_DAILY_PRICE_TR_ID,
        DAILY_PERIOD_DIV_CODE);
    JsonNode root =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(CURRENT_DAILY_PRICE_PATH)
                        .queryParam("FID_COND_MRKT_DIV_CODE", KRX_MARKET_DIV_CODE)
                        .queryParam("FID_INPUT_ISCD", ticker)
                        .queryParam("FID_PERIOD_DIV_CODE", DAILY_PERIOD_DIV_CODE)
                        .queryParam("FID_ORG_ADJ_PRC", ADJUSTED_PRICE)
                        .build())
            .headers(headers -> applyKisHeaders(headers, CURRENT_DAILY_PRICE_TR_ID))
            .retrieve()
            .onStatus(
                status -> status.isError(),
                response ->
                    response
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(
                            body ->
                                stockDailyPriceApiException(response.statusCode().value(), body)))
            .bodyToMono(String.class)
            .map(this::readDailyPriceTree)
            .timeout(REQUEST_TIMEOUT)
            .onErrorMap(this::mapStockDailyPriceException)
            .block();
    return requireKisResponse(root, StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED);
  }

  private void applyKisHeaders(HttpHeaders headers, String trId) {
    headers.setContentType(
        MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8"));
    headers.setBearerAuth(
        accessTokenClient.getAccessToken(
            kisProperties.getFinancialAppKeyOrDefault(),
            kisProperties.getFinancialAppSecretOrDefault(),
            kisProperties.getFinancialBaseUrlOrDefault()));
    headers.set("appkey", kisProperties.getFinancialAppKeyOrDefault());
    headers.set("appsecret", kisProperties.getFinancialAppSecretOrDefault());
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

  private RuntimeException stockDailyPriceApiException(int statusCode, String body) {
    log.warn(
        "code={}, message={}, status={}, responseBody={}",
        StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getMessage(),
        statusCode,
        body);
    return new StockException(StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED);
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

  private Throwable mapStockDailyPriceException(Throwable throwable) {
    if (throwable instanceof StockException) {
      return throwable;
    }
    log.warn(
        "code={}, message={}, exception={}, detail={}",
        StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED.getMessage(),
        throwable.getClass().getSimpleName(),
        throwable.getMessage());
    return new StockException(StockErrorCode.KIS_STOCK_DAILY_PRICE_API_FAILED, throwable);
  }

  private JsonNode requireKisResponse(JsonNode root, StockErrorCode errorCode) {
    if (root != null) {
      return root;
    }

    log.warn(
        "code={}, message={}, reason=empty-response", errorCode.getCode(), errorCode.getMessage());
    throw new StockException(errorCode);
  }

  private JsonNode readTree(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      log.atWarn()
          .setCause(e)
          .log(
              "code={}, message={}, responseBody={}",
              StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getCode(),
              StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED.getMessage(),
              body);
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED, e);
    }
  }

  private JsonNode readDailyPriceTree(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (JsonProcessingException e) {
      log.atWarn()
          .setCause(e)
          .log(
              "code={}, message={}, responseBody={}",
              StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED.getCode(),
              StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED.getMessage(),
              body);
      throw new StockException(StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED, e);
    }
  }

  private List<StockChartResDTO.DataPoint> fetchMinuteChartPoints(String ticker) {
    boolean includePastData = shouldIncludePastMinuteData();
    List<StockChartResDTO.DataPoint> points =
        fetchMinuteChartPoints(ticker, minuteChartInputHour(), includePastData);

    if (points.isEmpty() && !includePastData) {
      points = fetchMinuteChartPoints(ticker, MARKET_CLOSE_TIME.format(KIS_TIME_FORMATTER), true);
    }
    return points;
  }

  private List<StockChartResDTO.DataPoint> fetchMinuteChartPoints(
      String ticker, String firstInputHour, boolean includePastData) {
    List<StockChartResDTO.DataPoint> collected = new ArrayList<>();
    Set<String> seenTimes = new HashSet<>();
    LocalDate targetDate = null;
    LocalTime inputTime =
        parseTime(firstInputHour, StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED);

    for (int requestCount = 0; requestCount < MINUTE_CHART_MAX_REQUEST_COUNT; requestCount++) {
      JsonNode root;
      try {
        root =
            fetchValidMinuteChart(
                ticker, inputTime.format(KIS_TIME_FORMATTER), includePastData, requestCount);
      } catch (StockException e) {
        if (collected.isEmpty()) {
          throw e;
        }
        log.warn(
            "분봉 차트 추가 조회 실패로 수집된 데이터만 응답합니다. ticker={}, collectedCount={}",
            ticker,
            collected.size());
        break;
      }

      List<StockChartResDTO.DataPoint> pagePoints = toMinuteDataPoints(root.path("output2"));
      if (pagePoints.isEmpty()) {
        break;
      }

      if (targetDate == null) {
        targetDate = targetMinuteChartDate(pagePoints);
      }

      LocalTime earliestTime = null;
      for (StockChartResDTO.DataPoint point : pagePoints) {
        if (!targetDate.equals(pointDateTime(point).toLocalDate())) {
          continue;
        }

        LocalTime pointTime = pointDateTime(point).toLocalTime();
        if (pointTime.isBefore(MARKET_OPEN_TIME) || pointTime.isAfter(MARKET_CLOSE_TIME)) {
          continue;
        }

        if (seenTimes.add(point.time())) {
          collected.add(point);
        }
        if (earliestTime == null || pointTime.isBefore(earliestTime)) {
          earliestTime = pointTime;
        }
      }

      if (earliestTime == null || !earliestTime.isAfter(MARKET_OPEN_TIME)) {
        break;
      }

      LocalTime nextInputTime = earliestTime.minusMinutes(1);
      if (nextInputTime.isBefore(MARKET_OPEN_TIME)) {
        nextInputTime = MARKET_OPEN_TIME;
      }
      if (!nextInputTime.isBefore(inputTime)) {
        break;
      }
      inputTime = nextInputTime;
    }

    return collected;
  }

  private JsonNode fetchValidMinuteChart(
      String ticker, String inputHour, boolean includePastData, int requestCount) {
    waitBeforeMinuteChartRequest(requestCount);
    try {
      JsonNode root = fetchMinuteChart(ticker, inputHour, includePastData);
      validateKisChartResponse(root);
      return root;
    } catch (StockException e) {
      log.warn(
          "분봉 차트 API 호출 실패로 재시도합니다. ticker={}, inputHour={}, retryBackoffMs={}",
          ticker,
          inputHour,
          MINUTE_CHART_RETRY_BACKOFF.toMillis());
      sleep(MINUTE_CHART_RETRY_BACKOFF);
      JsonNode root = fetchMinuteChart(ticker, inputHour, includePastData);
      validateKisChartResponse(root);
      return root;
    }
  }

  private void waitBeforeMinuteChartRequest(int requestCount) {
    if (requestCount <= 0) {
      return;
    }
    sleep(MINUTE_CHART_PAGE_INTERVAL);
  }

  private void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED, e);
    }
  }

  private void validateKisChartResponse(JsonNode root) {
    if ("0".equals(root.path("rt_cd").asText())) {
      return;
    }

    log.warn(
        "code={}, message={}, msg_cd={}, msg={}",
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getCode(),
        StockErrorCode.KIS_STOCK_CHART_API_FAILED.getMessage(),
        root.path("msg_cd").asText(),
        root.path("msg1").asText());
    throw new StockException(StockErrorCode.KIS_STOCK_CHART_API_FAILED);
  }

  private LocalDate targetMinuteChartDate(List<StockChartResDTO.DataPoint> points) {
    Map<LocalDate, Integer> countByDate = new HashMap<>();
    for (StockChartResDTO.DataPoint point : points) {
      LocalDate date = pointDateTime(point).toLocalDate();
      countByDate.merge(date, 1, Integer::sum);
    }

    return countByDate.entrySet().stream()
        .max(
            Comparator.<Map.Entry<LocalDate, Integer>>comparingInt(Map.Entry::getValue)
                .thenComparing(Map.Entry::getKey))
        .map(Map.Entry::getKey)
        .orElseThrow(
            () -> new StockException(StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED));
  }

  private LocalDateTime pointDateTime(StockChartResDTO.DataPoint point) {
    try {
      return LocalDateTime.parse(point.time(), MINUTE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new StockException(StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED, e);
    }
  }

  private boolean shouldIncludePastMinuteData() {
    return LocalTime.now(KOREA_ZONE).withNano(0).isBefore(MARKET_OPEN_TIME);
  }

  private String minuteChartInputHour() {
    LocalTime now = LocalTime.now(KOREA_ZONE).withNano(0);
    if (now.isBefore(MARKET_OPEN_TIME)) {
      return MARKET_CLOSE_TIME.format(KIS_TIME_FORMATTER);
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

  private List<StockChartResDTO.DataPoint> displayPoints(
      List<StockChartResDTO.DataPoint> points, StockChartRange range) {
    if (range != StockChartRange.ONE_WEEK || points.size() <= ONE_WEEK_DISPLAY_POINT_COUNT) {
      return points;
    }
    return new ArrayList<>(
        points.subList(points.size() - ONE_WEEK_DISPLAY_POINT_COUNT, points.size()));
  }

  private List<StockDailyPriceResDTO.Item> toDailyPriceItems(JsonNode output) {
    List<StockDailyPriceResDTO.Item> items = new ArrayList<>();
    if (!output.isArray()) {
      return items;
    }

    for (JsonNode node : output) {
      Long closePrice =
          positiveLongValue(
              node, "stck_clpr", StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED);
      Long volume =
          positiveLongValue(
              node, "acml_vol", StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED);
      items.add(
          new StockDailyPriceResDTO.Item(
              dailyPriceDate(node),
              closePrice,
              signedDecimalValue(
                  node,
                  "prdy_ctrt",
                  textValue(node, "prdy_vrss_sign"),
                  StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED),
              volume,
              tradingValue(node)));
    }
    return items;
  }

  private String minuteTime(JsonNode node) {
    LocalDate date =
        parseDate(
            textValue(node, "stck_bsop_date"),
            StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED);
    LocalTime time =
        parseTime(
            textValue(node, "stck_cntg_hour"),
            StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED);
    return date.atTime(time.withSecond(0)).format(MINUTE_FORMATTER);
  }

  private String dateTime(JsonNode node) {
    return parseDate(
            textValue(node, "stck_bsop_date"), StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED)
        .format(DATE_FORMATTER);
  }

  private String dailyPriceDate(JsonNode node) {
    return parseDate(
            textValue(node, "stck_bsop_date"),
            StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED)
        .format(DATE_FORMATTER);
  }

  private LocalDate parseDate(String value, StockErrorCode parseErrorCode) {
    if (value == null) {
      throw new StockException(parseErrorCode);
    }

    try {
      return LocalDate.parse(value, KIS_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new StockException(parseErrorCode, e);
    }
  }

  private LocalTime parseTime(String value, StockErrorCode parseErrorCode) {
    if (value == null) {
      throw new StockException(parseErrorCode);
    }

    try {
      return LocalTime.parse(value, KIS_TIME_FORMATTER).withSecond(0);
    } catch (DateTimeParseException e) {
      throw new StockException(parseErrorCode, e);
    }
  }

  private Long positiveLongValue(JsonNode node, String fieldName) {
    return positiveLongValue(node, fieldName, StockErrorCode.KIS_STOCK_CHART_RESPONSE_PARSE_FAILED);
  }

  private Long positiveLongValue(JsonNode node, String fieldName, StockErrorCode parseErrorCode) {
    String value = textValue(node, fieldName);
    if (value == null) {
      throw new StockException(parseErrorCode);
    }

    try {
      return new BigDecimal(value.trim().replace(",", "")).abs().longValue();
    } catch (RuntimeException e) {
      throw new StockException(parseErrorCode, e);
    }
  }

  private BigDecimal signedDecimalValue(
      JsonNode node, String fieldName, String signCode, StockErrorCode parseErrorCode) {
    return applySign(decimalValue(node, fieldName, parseErrorCode), signCode);
  }

  private BigDecimal decimalValue(JsonNode node, String fieldName, StockErrorCode parseErrorCode) {
    String value = textValue(node, fieldName);
    if (value == null) {
      throw new StockException(parseErrorCode);
    }

    try {
      return new BigDecimal(value.trim().replace(",", ""));
    } catch (RuntimeException e) {
      throw new StockException(parseErrorCode, e);
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

  private Long tradingValue(JsonNode node) {
    String value = textValue(node, "acml_tr_pbmn");
    if (value == null) {
      return null;
    }
    return positiveLongValue(
        node, "acml_tr_pbmn", StockErrorCode.KIS_STOCK_DAILY_PRICE_RESPONSE_PARSE_FAILED);
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value;
  }

  private String normalizedBaseUrl(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
