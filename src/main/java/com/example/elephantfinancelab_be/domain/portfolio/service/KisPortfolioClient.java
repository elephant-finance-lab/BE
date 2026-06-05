package com.example.elephantfinancelab_be.domain.portfolio.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisAccessTokenClient;
import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import com.example.elephantfinancelab_be.domain.portfolio.exception.PortfolioException;
import com.example.elephantfinancelab_be.domain.portfolio.exception.code.PortfolioErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisPortfolioClient {

  private static final String BALANCE_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
  private static final String DAILY_TRADE_PATH =
      "/uapi/domestic-stock/v1/trading/inquire-daily-ccld";
  private static final String REAL_BALANCE_TR_ID = "TTTC8434R";
  private static final String VIRTUAL_BALANCE_TR_ID = "VTTC8434R";
  private static final String REAL_DAILY_TRADE_INNER_TR_ID = "TTTC0081R";
  private static final String VIRTUAL_DAILY_TRADE_INNER_TR_ID = "VTTC0081R";
  private static final String REAL_DAILY_TRADE_BEFORE_TR_ID = "CTSC9215R";
  private static final String VIRTUAL_DAILY_TRADE_BEFORE_TR_ID = "VTSC9215R";
  private static final String INITIAL_TR_CONT = "";
  private static final String NEXT_TR_CONT = "N";
  private static final int MAX_CONTINUOUS_REQUESTS = 10;
  private static final int MAX_FETCH_ATTEMPTS = 3;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
  private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter KIS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public Balance fetchBalance() {
    Account account = resolveAccount();
    List<Holding> holdings = new ArrayList<>();
    Totals totals = Totals.empty();
    String ctxAreaFk100 = "";
    String ctxAreaNk100 = "";
    String trCont = INITIAL_TR_CONT;

    for (int requestCount = 0; requestCount < MAX_CONTINUOUS_REQUESTS; requestCount++) {
      KisResponse response =
          fetch(
              BALANCE_PATH,
              balanceTrId(),
              balanceParams(account, ctxAreaFk100, ctxAreaNk100),
              trCont,
              "balance");
      JsonNode body = response.body();
      holdings.addAll(parseHoldings(body.path("output1")));
      Totals responseTotals = parseTotals(firstObject(body.path("output2")));
      if (!responseTotals.isEmpty()) {
        totals = responseTotals;
      }

      ctxAreaFk100 = textValue(body, "ctx_area_fk100");
      ctxAreaNk100 = textValue(body, "ctx_area_nk100");
      if (!hasNext(response.trCont())) {
        break;
      }
      trCont = NEXT_TR_CONT;
    }

    List<Holding> aggregatedHoldings = aggregateHoldings(holdings);
    return new Balance(aggregatedHoldings, completeTotals(totals, aggregatedHoldings));
  }

  public List<TradeItem> fetchTrades(LocalDate startDate, LocalDate endDate, TradeType type) {
    Account account = resolveAccount();
    List<TradeItem> trades = new ArrayList<>();
    String ctxAreaFk100 = "";
    String ctxAreaNk100 = "";
    String trCont = INITIAL_TR_CONT;

    for (int requestCount = 0; requestCount < MAX_CONTINUOUS_REQUESTS; requestCount++) {
      KisResponse response =
          fetch(
              DAILY_TRADE_PATH,
              dailyTradeTrId(startDate, endDate),
              dailyTradeParams(account, startDate, endDate, type, ctxAreaFk100, ctxAreaNk100),
              trCont,
              "daily-trades");
      JsonNode output = response.body().path("output1");
      if (!output.isArray()) {
        output = response.body().path("output");
      }
      trades.addAll(parseTrades(output, type, endDate));

      ctxAreaFk100 = textValue(response.body(), "ctx_area_fk100");
      ctxAreaNk100 = textValue(response.body(), "ctx_area_nk100");
      if (!hasNext(response.trCont())) {
        break;
      }
      trCont = NEXT_TR_CONT;
    }

    return trades.stream().sorted((a, b) -> b.tradedAt().compareTo(a.tradedAt())).toList();
  }

  private KisResponse fetch(
      String path, String trId, Map<String, String> params, String trCont, String apiName) {
    try {
      for (int attempt = 0; attempt < MAX_FETCH_ATTEMPTS; attempt++) {
        HttpRequest.Builder builder =
            HttpRequest.newBuilder()
                .uri(uri(path, params))
                .timeout(REQUEST_TIMEOUT)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8")
                .header("authorization", "Bearer " + accessTokenClient.getAccessToken())
                .header("appkey", kisProperties.getAppKey())
                .header("appsecret", kisProperties.getAppSecret())
                .header("tr_id", trId)
                .header("custtype", "P")
                .GET();
        if (hasText(trCont)) {
          builder.header("tr_cont", trCont);
        }

        HttpResponse<String> response =
            httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          if (isExpiredToken(response.body()) && attempt == 0) {
            accessTokenClient.invalidateAccessToken();
            continue;
          }
          if (isRateLimited(response.body()) && attempt < MAX_FETCH_ATTEMPTS - 1) {
            sleepBeforeRetry(attempt);
            continue;
          }
          log.warn(
              "code={}, message={}, api={}, trId={}, status={}, account={}, kisError={}",
              PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED.getCode(),
              PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED.getMessage(),
              apiName,
              trId,
              response.statusCode(),
              kisProperties.maskedAccount(),
              kisErrorSummary(response.body()));
          throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED);
        }

        JsonNode root = readTree(response.body(), apiName);
        if (!"0".equals(root.path("rt_cd").asText())) {
          if (isExpiredToken(root) && attempt == 0) {
            accessTokenClient.invalidateAccessToken();
            continue;
          }
          if (isRateLimited(root) && attempt < MAX_FETCH_ATTEMPTS - 1) {
            sleepBeforeRetry(attempt);
            continue;
          }
          log.warn(
              "code={}, message={}, api={}, trId={}, msgCd={}, msg={}, account={}",
              PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED.getCode(),
              PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED.getMessage(),
              apiName,
              trId,
              root.path("msg_cd").asText(),
              root.path("msg1").asText(),
              kisProperties.maskedAccount());
          throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED);
        }

        return new KisResponse(root, response.headers().firstValue("tr_cont").orElse(""));
      }
    } catch (IOException e) {
      throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED, e);
    }
    throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_API_FAILED);
  }

  private JsonNode readTree(String body, String apiName) {
    try {
      return objectMapper.readTree(body);
    } catch (IOException e) {
      log.warn(
          "code={}, message={}, api={}, account={}",
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getCode(),
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getMessage(),
          apiName,
          kisProperties.maskedAccount());
      throw new PortfolioException(PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED, e);
    }
  }

  private String kisErrorSummary(String body) {
    if (body == null || body.isBlank()) {
      return "empty";
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String msgCd = root.path("msg_cd").asText("");
      String msg = root.path("msg1").asText("");
      if (!msgCd.isBlank() || !msg.isBlank()) {
        return "msg_cd=" + msgCd + ", msg1=" + msg;
      }
    } catch (IOException ignored) {
      // Fall through to a compact body preview for non-JSON KIS errors.
    }
    String compact = body.replaceAll("\\s+", " ").trim();
    return compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
  }

  private boolean isExpiredToken(String body) {
    if (body == null || body.isBlank()) {
      return false;
    }
    try {
      return isExpiredToken(objectMapper.readTree(body));
    } catch (IOException ignored) {
      return false;
    }
  }

  private boolean isExpiredToken(JsonNode root) {
    return "EGW00123".equals(root.path("msg_cd").asText());
  }

  private boolean isRateLimited(String body) {
    if (body == null || body.isBlank()) {
      return false;
    }
    try {
      return isRateLimited(objectMapper.readTree(body));
    } catch (IOException ignored) {
      return false;
    }
  }

  private boolean isRateLimited(JsonNode root) {
    return "EGW00201".equals(root.path("msg_cd").asText());
  }

  private void sleepBeforeRetry(int attempt) throws InterruptedException {
    Thread.sleep(500L * (attempt + 1));
  }

  private Account resolveAccount() {
    if (!kisProperties.hasAccount()) {
      log.warn(
          "code={}, message={}, account={}",
          PortfolioErrorCode.KIS_ACCOUNT_CONFIG_MISSING.getCode(),
          PortfolioErrorCode.KIS_ACCOUNT_CONFIG_MISSING.getMessage(),
          kisProperties.maskedAccount());
      throw new PortfolioException(PortfolioErrorCode.KIS_ACCOUNT_CONFIG_MISSING);
    }
    return new Account(kisProperties.getCanoOrDefault(), kisProperties.getAcntPrdtCdOrDefault());
  }

  private Map<String, String> balanceParams(
      Account account, String ctxAreaFk100, String ctxAreaNk100) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("CANO", account.cano());
    params.put("ACNT_PRDT_CD", account.acntPrdtCd());
    params.put("AFHR_FLPR_YN", "N");
    params.put("OFL_YN", "");
    params.put("INQR_DVSN", "01");
    params.put("UNPR_DVSN", "01");
    params.put("FUND_STTL_ICLD_YN", "N");
    params.put("FNCG_AMT_AUTO_RDPT_YN", "N");
    params.put("PRCS_DVSN", "00");
    params.put("CTX_AREA_FK100", ctxAreaFk100);
    params.put("CTX_AREA_NK100", ctxAreaNk100);
    return params;
  }

  private Map<String, String> dailyTradeParams(
      Account account,
      LocalDate startDate,
      LocalDate endDate,
      TradeType type,
      String ctxAreaFk100,
      String ctxAreaNk100) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("CANO", account.cano());
    params.put("ACNT_PRDT_CD", account.acntPrdtCd());
    params.put("INQR_STRT_DT", startDate.format(KIS_DATE_FORMATTER));
    params.put("INQR_END_DT", endDate.format(KIS_DATE_FORMATTER));
    params.put("SLL_BUY_DVSN_CD", sideCode(type));
    params.put("PDNO", "");
    params.put("CCLD_DVSN", "01");
    params.put("INQR_DVSN", "00");
    params.put("INQR_DVSN_3", "00");
    params.put("ORD_GNO_BRNO", "");
    params.put("ODNO", "");
    params.put("INQR_DVSN_1", "");
    params.put("CTX_AREA_FK100", ctxAreaFk100);
    params.put("CTX_AREA_NK100", ctxAreaNk100);
    params.put("EXCG_ID_DVSN_CD", "KRX");
    return params;
  }

  private List<Holding> parseHoldings(JsonNode output) {
    List<Holding> holdings = new ArrayList<>();
    if (!output.isArray()) {
      return holdings;
    }

    for (JsonNode node : output) {
      int quantity = (int) longValue(node, "hldg_qty");
      if (quantity <= 0) {
        continue;
      }
      String stockCode = textValue(node, "pdno");
      if (!hasText(stockCode)) {
        continue;
      }
      String stockName = textValue(node, "prdt_name");
      long purchaseAmount = longValue(node, "pchs_amt");
      long averagePrice = roundedLongValue(node, "pchs_avg_pric");
      long currentPrice = longValue(node, "prpr");
      long evaluationAmount = longValue(node, "evlu_amt");
      if (evaluationAmount <= 0 && currentPrice > 0) {
        evaluationAmount = currentPrice * quantity;
      }
      if (currentPrice <= 0 && evaluationAmount > 0) {
        currentPrice = Math.round((double) evaluationAmount / quantity);
      }
      long profitLossAmount = longValue(node, "evlu_pfls_amt");
      if (profitLossAmount == 0 && evaluationAmount > 0 && purchaseAmount > 0) {
        profitLossAmount = evaluationAmount - purchaseAmount;
      }
      double profitLossRate = doubleValue(node, "evlu_pfls_rt");
      if (profitLossRate == 0.0 && purchaseAmount > 0) {
        profitLossRate = percent(profitLossAmount, purchaseAmount);
      }

      holdings.add(
          new Holding(
              stockCode,
              hasText(stockName) ? stockName : stockCode,
              quantity,
              averagePrice,
              currentPrice,
              purchaseAmount,
              evaluationAmount,
              profitLossAmount,
              profitLossRate));
    }

    return holdings;
  }

  private Totals parseTotals(JsonNode output2) {
    if (output2.isMissingNode() || output2.isNull()) {
      return Totals.empty();
    }
    long purchaseAmount = longValue(output2, "pchs_amt_smtl_amt", "pchs_amt_smtl");
    long stockEvaluationAmount =
        longValue(output2, "evlu_amt_smtl_amt", "scts_evlu_amt", "evlu_amt_smtl");
    long cashAmount = longValue(output2, "dnca_tot_amt", "prvs_rcdl_excc_amt", "tot_dncl_amt");
    long totalAssetAmount = longValue(output2, "tot_evlu_amt", "tot_asst_amt", "nass_amt");
    long totalProfitLossAmount = longValue(output2, "evlu_pfls_smtl_amt", "evlu_pfls_amt_smtl");
    double totalProfitLossRate =
        purchaseAmount == 0 ? 0.0 : percent(totalProfitLossAmount, purchaseAmount);
    return new Totals(
        purchaseAmount,
        stockEvaluationAmount,
        cashAmount,
        totalAssetAmount,
        totalProfitLossAmount,
        totalProfitLossRate);
  }

  private List<TradeItem> parseTrades(
      JsonNode output, TradeType requestedType, LocalDate fallbackDate) {
    List<TradeItem> trades = new ArrayList<>();
    if (!output.isArray()) {
      return trades;
    }

    for (JsonNode node : output) {
      TradeType type = tradeType(node);
      if (type == null || (requestedType != null && requestedType != type)) {
        continue;
      }
      int quantity = (int) longValue(node, "tot_ccld_qty", "ord_qty");
      if (quantity <= 0) {
        continue;
      }
      String stockCode = firstText(node, "pdno", "shtn_pdno");
      if (!hasText(stockCode)) {
        continue;
      }
      String stockName = firstText(node, "prdt_name", "prdt_abrv_name");
      long price = longValue(node, "avg_prvs", "ord_unpr");
      long amount = longValue(node, "tot_ccld_amt");
      if (price <= 0 && amount > 0) {
        price = Math.round((double) amount / quantity);
      }
      if (amount <= 0 && price > 0) {
        amount = price * quantity;
      }

      trades.add(
          new TradeItem(
              stockCode,
              hasText(stockName) ? stockName : stockCode,
              type,
              quantity,
              price,
              amount,
              tradedAt(node, fallbackDate)));
    }

    return trades;
  }

  private List<Holding> aggregateHoldings(List<Holding> source) {
    Map<String, Holding> holdingsByCode = new LinkedHashMap<>();
    for (Holding holding : source) {
      Holding previous = holdingsByCode.get(holding.stockCode());
      if (previous == null) {
        holdingsByCode.put(holding.stockCode(), holding);
        continue;
      }
      int quantity = previous.quantity() + holding.quantity();
      long purchaseAmount = previous.purchaseAmount() + holding.purchaseAmount();
      long evaluationAmount = previous.evaluationAmount() + holding.evaluationAmount();
      long profitLossAmount = previous.profitLossAmount() + holding.profitLossAmount();
      long averagePrice = quantity == 0 ? 0 : Math.round((double) purchaseAmount / quantity);
      long currentPrice = quantity == 0 ? 0 : Math.round((double) evaluationAmount / quantity);
      holdingsByCode.put(
          holding.stockCode(),
          new Holding(
              holding.stockCode(),
              previous.stockName(),
              quantity,
              averagePrice,
              currentPrice,
              purchaseAmount,
              evaluationAmount,
              profitLossAmount,
              purchaseAmount == 0 ? 0.0 : percent(profitLossAmount, purchaseAmount)));
    }
    return new ArrayList<>(holdingsByCode.values());
  }

  private Totals completeTotals(Totals totals, List<Holding> holdings) {
    long purchaseAmount =
        totals.purchaseAmount() > 0
            ? totals.purchaseAmount()
            : holdings.stream().mapToLong(Holding::purchaseAmount).sum();
    long stockEvaluationAmount =
        totals.stockEvaluationAmount() > 0
            ? totals.stockEvaluationAmount()
            : holdings.stream().mapToLong(Holding::evaluationAmount).sum();
    long cashAmount = totals.cashAmount();
    long totalAssetAmount =
        totals.totalAssetAmount() > 0
            ? totals.totalAssetAmount()
            : stockEvaluationAmount + cashAmount;
    long totalProfitLossAmount =
        totals.totalProfitLossAmount() != 0
            ? totals.totalProfitLossAmount()
            : holdings.stream().mapToLong(Holding::profitLossAmount).sum();
    double totalProfitLossRate =
        totals.totalProfitLossRate() != 0.0
            ? totals.totalProfitLossRate()
            : purchaseAmount == 0 ? 0.0 : percent(totalProfitLossAmount, purchaseAmount);
    return new Totals(
        purchaseAmount,
        stockEvaluationAmount,
        cashAmount,
        totalAssetAmount,
        totalProfitLossAmount,
        totalProfitLossRate);
  }

  private URI uri(String path, Map<String, String> params) {
    String baseUrl = kisProperties.getBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return URI.create(normalizedBaseUrl + path + "?" + queryString(params));
  }

  private String queryString(Map<String, String> params) {
    StringJoiner joiner = new StringJoiner("&");
    params.forEach(
        (key, value) -> joiner.add(encode(key) + "=" + encode(value == null ? "" : value)));
    return joiner.toString();
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String balanceTrId() {
    return isVirtual() ? VIRTUAL_BALANCE_TR_ID : REAL_BALANCE_TR_ID;
  }

  private String dailyTradeTrId(LocalDate startDate, LocalDate endDate) {
    boolean beforeThreeMonths = startDate.isBefore(endDate.minusMonths(3));
    if (isVirtual()) {
      return beforeThreeMonths ? VIRTUAL_DAILY_TRADE_BEFORE_TR_ID : VIRTUAL_DAILY_TRADE_INNER_TR_ID;
    }
    return beforeThreeMonths ? REAL_DAILY_TRADE_BEFORE_TR_ID : REAL_DAILY_TRADE_INNER_TR_ID;
  }

  private boolean isVirtual() {
    return kisProperties.getMode() == KisProperties.Mode.VIRTUAL;
  }

  private String sideCode(TradeType type) {
    if (type == TradeType.SELL) {
      return "01";
    }
    if (type == TradeType.BUY) {
      return "02";
    }
    return "00";
  }

  private boolean hasNext(String trCont) {
    return "M".equalsIgnoreCase(trCont) || "F".equalsIgnoreCase(trCont);
  }

  private JsonNode firstObject(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return MissingNode.getInstance();
    }
    if (node.isArray()) {
      return node.size() > 0 ? node.get(0) : MissingNode.getInstance();
    }
    return node;
  }

  private TradeType tradeType(JsonNode node) {
    String sideCode = textValue(node, "sll_buy_dvsn_cd");
    if ("01".equals(sideCode)) {
      return TradeType.SELL;
    }
    if ("02".equals(sideCode)) {
      return TradeType.BUY;
    }

    String sideName = firstText(node, "sll_buy_dvsn_cd_name", "sll_buy_dvsn_name");
    if (sideName != null && sideName.contains("매도")) {
      return TradeType.SELL;
    }
    if (sideName != null && sideName.contains("매수")) {
      return TradeType.BUY;
    }
    return null;
  }

  private LocalDateTime tradedAt(JsonNode node, LocalDate fallbackDate) {
    LocalDate date = parseDate(textValue(node, "ord_dt"), fallbackDate);
    LocalTime time = parseTime(textValue(node, "ord_tmd"));
    return LocalDateTime.of(date, time);
  }

  private LocalDate parseDate(String value, LocalDate fallbackDate) {
    if (!hasText(value)) {
      return fallbackDate;
    }
    try {
      return LocalDate.parse(value, KIS_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      return fallbackDate;
    }
  }

  private LocalTime parseTime(String value) {
    if (!hasText(value)) {
      return LocalTime.MIDNIGHT;
    }
    try {
      return LocalTime.parse(value, KIS_TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      return LocalTime.MIDNIGHT;
    }
  }

  private String firstText(JsonNode node, String firstField, String secondField) {
    String first = textValue(node, firstField);
    return hasText(first) ? first : textValue(node, secondField);
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return hasText(value) ? value.trim() : "";
  }

  private long longValue(JsonNode node, String... fieldNames) {
    for (String fieldName : fieldNames) {
      String value = textValue(node, fieldName);
      if (!hasText(value)) {
        continue;
      }
      try {
        return new BigDecimal(normalizeNumeric(value)).longValue();
      } catch (NumberFormatException e) {
        log.warn(
            "code={}, message={}, field={}, value={}",
            PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getCode(),
            PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getMessage(),
            fieldName,
            value);
      }
    }
    return 0L;
  }

  private long roundedLongValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    if (!hasText(value)) {
      return 0L;
    }
    try {
      return new BigDecimal(normalizeNumeric(value)).setScale(0, RoundingMode.HALF_UP).longValue();
    } catch (NumberFormatException e) {
      log.warn(
          "code={}, message={}, field={}, value={}",
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getCode(),
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getMessage(),
          fieldName,
          value);
      return 0L;
    }
  }

  private double doubleValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    if (!hasText(value)) {
      return 0.0;
    }
    try {
      return roundTwoDecimals(new BigDecimal(normalizeNumeric(value)).doubleValue());
    } catch (NumberFormatException e) {
      log.warn(
          "code={}, message={}, field={}, value={}",
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getCode(),
          PortfolioErrorCode.KIS_PORTFOLIO_RESPONSE_PARSE_FAILED.getMessage(),
          fieldName,
          value);
      return 0.0;
    }
  }

  private double percent(long numerator, long denominator) {
    if (denominator == 0) {
      return 0.0;
    }
    return roundTwoDecimals((double) numerator / denominator * 100.0);
  }

  private double roundTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private String normalizeNumeric(String value) {
    return value.trim().replace(",", "");
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record Account(String cano, String acntPrdtCd) {}

  private record KisResponse(JsonNode body, String trCont) {}

  public record Balance(List<Holding> holdings, Totals totals) {}

  public record Holding(
      String stockCode,
      String stockName,
      int quantity,
      long averagePrice,
      long currentPrice,
      long purchaseAmount,
      long evaluationAmount,
      long profitLossAmount,
      double profitLossRate) {}

  public record Totals(
      long purchaseAmount,
      long stockEvaluationAmount,
      long cashAmount,
      long totalAssetAmount,
      long totalProfitLossAmount,
      double totalProfitLossRate) {

    static Totals empty() {
      return new Totals(0L, 0L, 0L, 0L, 0L, 0.0);
    }

    boolean isEmpty() {
      return purchaseAmount == 0
          && stockEvaluationAmount == 0
          && cashAmount == 0
          && totalAssetAmount == 0
          && totalProfitLossAmount == 0
          && totalProfitLossRate == 0.0;
    }
  }

  public record TradeItem(
      String stockCode,
      String stockName,
      TradeType type,
      int quantity,
      long price,
      long totalAmount,
      LocalDateTime tradedAt) {}
}
