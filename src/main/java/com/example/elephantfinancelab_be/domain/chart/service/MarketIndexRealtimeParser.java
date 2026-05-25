package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketIndexRealtimeParser {

  static final String DOMESTIC_INDEX_REALTIME_TR_ID = "H0UPCNT0";

  private static final String PARSE_FAILED_LOG = "market index parse failed, keep previous value";
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter KIS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
  private static final Duration FUTURE_TIMESTAMP_TOLERANCE = Duration.ofHours(1);
  private static final int INDEX_CODE_FIELD = 0;
  private static final int TRADE_TIME_FIELD = 1;
  private static final int CURRENT_VALUE_FIELD = 2;
  private static final int CHANGE_SIGN_FIELD = 3;
  private static final int CHANGE_FIELD = 4;
  private static final int CHANGE_RATE_FIELD = 9;
  private static final int RESPONSE_FIELD_COUNT = 30;

  public Optional<ParsedMarketIndex> parse(String message) {
    return parseAll(message).stream().findFirst();
  }

  public List<ParsedMarketIndex> parseAll(String message) {
    if (message == null || message.isBlank()) {
      log.warn("{}, reason=empty-message", PARSE_FAILED_LOG);
      return List.of();
    }

    if (message.startsWith("{")) {
      return List.of();
    }

    String[] parts = message.split("\\|", 4);
    if (parts.length < 4) {
      log.warn("{}, reason=malformed-frame", PARSE_FAILED_LOG);
      return List.of();
    }

    if (!DOMESTIC_INDEX_REALTIME_TR_ID.equals(parts[1])) {
      return List.of();
    }

    if ("1".equals(parts[0])) {
      log.warn(
          "{}, code={}, message={}, reason=encrypted",
          PARSE_FAILED_LOG,
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage());
      return List.of();
    }

    String[] fields = parts[3].split("\\^", -1);
    if (fields.length <= CHANGE_RATE_FIELD) {
      log.warn(
          "{}, code={}, message={}, count={}, expectedAtLeast={}",
          PARSE_FAILED_LOG,
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
          fields.length,
          CHANGE_RATE_FIELD + 1);
      return List.of();
    }

    int dataCount = parseDataCount(parts[2]);
    List<ParsedMarketIndex> indexes = new ArrayList<>();
    for (int index = 0; index < dataCount; index++) {
      int offset = index * RESPONSE_FIELD_COUNT;
      if (fields.length <= offset + CHANGE_RATE_FIELD) {
        log.warn(
            "{}, code={}, message={}, item={}, count={}, expectedAtLeast={}",
            PARSE_FAILED_LOG,
            ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
            ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
            index,
            fields.length,
            offset + CHANGE_RATE_FIELD + 1);
        break;
      }

      String kisIndexCode = fields[offset + INDEX_CODE_FIELD];
      Optional<MarketIndexMarket> market = MarketIndexMarket.fromKisIndexCode(kisIndexCode);
      if (market.isEmpty()) {
        log.warn("{}, reason=unsupported-market, kisIndexCode={}", PARSE_FAILED_LOG, kisIndexCode);
        continue;
      }

      toMarketIndex(market.get(), fields, offset)
          .map(indexDto -> new ParsedMarketIndex(market.get(), indexDto))
          .ifPresent(indexes::add);
    }

    return indexes;
  }

  private Optional<MarketIndexResDTO.MarketIndex> toMarketIndex(
      MarketIndexMarket market, String[] fields, int offset) {
    String changeSign = fields[offset + CHANGE_SIGN_FIELD];
    if (!hasText(changeSign)) {
      log.warn("{}, market={}, reason=blank-change-sign", PARSE_FAILED_LOG, market.name());
      return Optional.empty();
    }

    Optional<BigDecimal> value =
        parseDecimal(market, "value", fields[offset + CURRENT_VALUE_FIELD]);
    Optional<BigDecimal> change = parseDecimal(market, "change", fields[offset + CHANGE_FIELD]);
    Optional<BigDecimal> changeRate =
        parseDecimal(market, "changeRate", fields[offset + CHANGE_RATE_FIELD]);

    if (value.isEmpty() || change.isEmpty() || changeRate.isEmpty()) {
      return Optional.empty();
    }

    if (value.get().compareTo(BigDecimal.ZERO) <= 0) {
      log.warn(
          "{}, market={}, reason=non-positive-value, value={}",
          PARSE_FAILED_LOG,
          market.name(),
          value.get());
      return Optional.empty();
    }

    return Optional.of(
        new MarketIndexResDTO.MarketIndex(
            market.name(),
            value.get(),
            applySign(change.get(), changeSign),
            applySign(changeRate.get(), changeSign),
            parseTimestamp(fields[offset + TRADE_TIME_FIELD])));
  }

  private int parseDataCount(String dataCount) {
    try {
      int count = Integer.parseInt(dataCount);
      if (count <= 0) {
        log.warn("{}, reason=invalid-data-count, dataCount={}", PARSE_FAILED_LOG, dataCount);
        return 0;
      }
      return count;
    } catch (NumberFormatException e) {
      log.warn("{}, reason=invalid-data-count, dataCount={}", PARSE_FAILED_LOG, dataCount);
      return 0;
    }
  }

  private Optional<BigDecimal> parseDecimal(
      MarketIndexMarket market, String fieldName, String value) {
    if (!hasText(value)) {
      log.warn(
          "{}, market={}, reason=blank-decimal, field={}",
          PARSE_FAILED_LOG,
          market.name(),
          fieldName);
      return Optional.empty();
    }

    try {
      return Optional.of(new BigDecimal(value.trim().replace(",", "")));
    } catch (NumberFormatException e) {
      log.warn(
          "{}, code={}, message={}, market={}, field={}, value={}",
          PARSE_FAILED_LOG,
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
          market.name(),
          fieldName,
          value);
      return Optional.empty();
    }
  }

  private BigDecimal applySign(BigDecimal value, String signCode) {
    if (signCode == null) {
      return value;
    }

    // FLAT sign means no index movement, so ignore any non-zero raw delta from KIS.
    return switch (signCode) {
      case "4", "5", "8", "9" -> value.abs().negate();
      case "3" -> BigDecimal.ZERO;
      default -> value.abs();
    };
  }

  private LocalDateTime parseTimestamp(String kisTime) {
    LocalDate today = LocalDate.now(KOREA_ZONE);
    if (kisTime == null || kisTime.isBlank()) {
      LocalDateTime receivedAt = LocalDateTime.now(KOREA_ZONE).withNano(0);
      log.warn("market index timestamp missing, use received time: timestamp={}", receivedAt);
      return receivedAt;
    }

    try {
      LocalDateTime now = LocalDateTime.now(KOREA_ZONE).withNano(0);
      LocalDateTime timestamp =
          LocalDateTime.of(today, LocalTime.parse(kisTime, KIS_TIME_FORMATTER));
      if (timestamp.isAfter(now.plus(FUTURE_TIMESTAMP_TOLERANCE))) {
        return timestamp.minusDays(1);
      }
      return timestamp;
    } catch (DateTimeParseException e) {
      LocalDateTime receivedAt = LocalDateTime.now(KOREA_ZONE).withNano(0);
      log.warn(
          "market index timestamp parse failed, use received time: value={}, timestamp={}",
          kisTime,
          receivedAt);
      return receivedAt;
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public record ParsedMarketIndex(MarketIndexMarket market, MarketIndexResDTO.MarketIndex index) {}
}
