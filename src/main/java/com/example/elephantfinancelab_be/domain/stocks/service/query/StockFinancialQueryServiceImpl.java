package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialApiType;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockFinancialClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockFinancialRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockFinancialQueryServiceImpl implements StockFinancialQueryService {

  private static final int COLUMN_COUNT = 4;
  private static final String PERIOD_FIELD_NAME = "stac_yymm";

  private final StockRepository stockRepository;
  private final StockFinancialRedisService stockFinancialRedisService;
  private final KisStockFinancialClient kisStockFinancialClient;

  @Override
  public StockFinancialResDTO.Financial getFinancial(
      String ticker, String statement, String period) {
    StockFinancialStatement financialStatement = StockFinancialStatement.from(statement);
    StockFinancialPeriod financialPeriod = StockFinancialPeriod.from(period);
    Stock stock =
        stockRepository
            .findByTicker(normalizeTicker(ticker))
            .orElseThrow(() -> new StockException(StockErrorCode.STOCK_NOT_FOUND));

    StockFinancialResDTO.Financial cachedFinancial =
        findCachedFinancial(stock, financialStatement, financialPeriod);
    if (cachedFinancial != null) {
      log.info(
          "종목 재무제표 캐시 조회 성공. ticker={}, statement={}, period={}",
          stock.getTicker(),
          financialStatement,
          financialPeriod);
      return cachedFinancial;
    }

    Map<StockFinancialApiType, List<JsonNode>> outputByApi =
        fetchOutputByApi(stock.getTicker(), financialStatement, financialPeriod);
    StockFinancialResDTO.Financial financial =
        toFinancialResponse(stock, financialStatement, financialPeriod, outputByApi);
    saveFinancialCache(stock, financial);
    return financial;
  }

  private Map<StockFinancialApiType, List<JsonNode>> fetchOutputByApi(
      String ticker, StockFinancialStatement statement, StockFinancialPeriod period) {
    Map<StockFinancialApiType, List<JsonNode>> outputByApi =
        new EnumMap<>(StockFinancialApiType.class);
    statement.getMetrics().stream()
        .map(StockFinancialStatement.Metric::apiType)
        .distinct()
        .forEach(
            apiType ->
                outputByApi.put(
                    apiType, kisStockFinancialClient.fetchFinancial(ticker, apiType, period)));
    return outputByApi;
  }

  private StockFinancialResDTO.Financial toFinancialResponse(
      Stock stock,
      StockFinancialStatement statement,
      StockFinancialPeriod period,
      Map<StockFinancialApiType, List<JsonNode>> outputByApi) {
    List<String> periodKeys = latestPeriodKeys(outputByApi);
    List<String> columns =
        periodKeys.stream().map(periodKey -> columnLabel(periodKey, period)).toList();
    Map<StockFinancialApiType, Map<String, JsonNode>> nodeByApiAndPeriod =
        nodeByApiAndPeriod(outputByApi);
    List<StockFinancialResDTO.Row> rows =
        statement.getMetrics().stream()
            .map(metric -> toRow(metric, periodKeys, nodeByApiAndPeriod))
            .toList();

    return new StockFinancialResDTO.Financial(
        stock.getTicker(), stock.getName(), statement, period, statement.getUnit(), columns, rows);
  }

  private List<String> latestPeriodKeys(Map<StockFinancialApiType, List<JsonNode>> outputByApi) {
    return outputByApi.values().stream()
        .flatMap(List::stream)
        .map(node -> textValue(node, PERIOD_FIELD_NAME))
        .filter(Objects::nonNull)
        .distinct()
        .sorted(Comparator.reverseOrder())
        .limit(COLUMN_COUNT)
        .sorted()
        .toList();
  }

  private Map<StockFinancialApiType, Map<String, JsonNode>> nodeByApiAndPeriod(
      Map<StockFinancialApiType, List<JsonNode>> outputByApi) {
    Map<StockFinancialApiType, Map<String, JsonNode>> result =
        new EnumMap<>(StockFinancialApiType.class);
    outputByApi.forEach(
        (apiType, nodes) -> {
          Map<String, JsonNode> byPeriod = new LinkedHashMap<>();
          nodes.forEach(
              node -> {
                String periodKey = textValue(node, PERIOD_FIELD_NAME);
                if (periodKey != null) {
                  byPeriod.putIfAbsent(periodKey, node);
                }
              });
          result.put(apiType, byPeriod);
        });
    return result;
  }

  private StockFinancialResDTO.Row toRow(
      StockFinancialStatement.Metric metric,
      List<String> periodKeys,
      Map<StockFinancialApiType, Map<String, JsonNode>> nodeByApiAndPeriod) {
    Map<String, JsonNode> nodeByPeriod =
        nodeByApiAndPeriod.getOrDefault(metric.apiType(), Map.of());
    List<String> values =
        periodKeys.stream()
            .map(periodKey -> nodeByPeriod.get(periodKey))
            .map(node -> node == null ? "" : financialValue(node, metric.fieldName()))
            .toList();
    return new StockFinancialResDTO.Row(metric.label(), values);
  }

  private String columnLabel(String periodKey, StockFinancialPeriod period) {
    if (periodKey.length() != 6) {
      return periodKey;
    }

    String year = periodKey.substring(2, 4) + "년";
    if (period == StockFinancialPeriod.YEAR) {
      return year;
    }

    try {
      int month = Integer.parseInt(periodKey.substring(4, 6));
      int quarter = Math.max(1, Math.min(4, (month - 1) / 3 + 1));
      return year + " " + quarter + "Q";
    } catch (NumberFormatException e) {
      return periodKey;
    }
  }

  private String financialValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    if (value == null) {
      return "";
    }

    try {
      return new BigDecimal(value.trim().replace(",", "")).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException e) {
      return value;
    }
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value;
  }

  private StockFinancialResDTO.Financial findCachedFinancial(
      Stock stock, StockFinancialStatement statement, StockFinancialPeriod period) {
    try {
      return stockFinancialRedisService.find(stock.getTicker(), statement, period);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, statement={}, period={}",
          StockErrorCode.STOCK_FINANCIAL_CACHE_DESERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_FINANCIAL_CACHE_DESERIALIZE_FAILED.getMessage(),
          stock.getTicker(),
          statement,
          period,
          e);
      return null;
    }
  }

  private void saveFinancialCache(Stock stock, StockFinancialResDTO.Financial financial) {
    try {
      stockFinancialRedisService.save(financial);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, statement={}, period={}",
          StockErrorCode.STOCK_FINANCIAL_CACHE_SERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_FINANCIAL_CACHE_SERIALIZE_FAILED.getMessage(),
          stock.getTicker(),
          financial.statement(),
          financial.period(),
          e);
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.STOCK_NOT_FOUND);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
