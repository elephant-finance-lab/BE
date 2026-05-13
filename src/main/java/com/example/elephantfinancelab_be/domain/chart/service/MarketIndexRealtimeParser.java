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
    if (message == null || message.isBlank() || message.startsWith("{")) {
      return List.of();
    }

    String[] parts = message.split("\\|", 4);
    if (parts.length < 4 || !DOMESTIC_INDEX_REALTIME_TR_ID.equals(parts[1])) {
      return List.of();
    }

    if ("1".equals(parts[0])) {
      log.warn(
          "code={}, message={}, reason=encrypted",
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage());
      return List.of();
    }

    String[] fields = parts[3].split("\\^", -1);
    if (fields.length <= CHANGE_RATE_FIELD) {
      log.warn(
          "code={}, message={}, count={}",
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
          fields.length);
      return List.of();
    }

    int dataCount = parseDataCount(parts[2]);
    List<ParsedMarketIndex> indexes = new ArrayList<>();
    for (int index = 0; index < dataCount; index++) {
      int offset = index * RESPONSE_FIELD_COUNT;
      if (fields.length <= offset + CHANGE_RATE_FIELD) {
        log.warn(
            "code={}, message={}, item={}, count={}",
            ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
            ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
            index,
            fields.length);
        break;
      }

      MarketIndexMarket.fromKisIndexCode(fields[offset + INDEX_CODE_FIELD])
          .map(market -> new ParsedMarketIndex(market, toMarketIndex(market, fields, offset)))
          .ifPresent(indexes::add);
    }

    return indexes;
  }

  private MarketIndexResDTO.MarketIndex toMarketIndex(
      MarketIndexMarket market, String[] fields, int offset) {
    String changeSign = fields[offset + CHANGE_SIGN_FIELD];
    return new MarketIndexResDTO.MarketIndex(
        market.name(),
        parseDecimal(fields[offset + CURRENT_VALUE_FIELD]),
        applySign(parseDecimal(fields[offset + CHANGE_FIELD]), changeSign),
        applySign(parseDecimal(fields[offset + CHANGE_RATE_FIELD]), changeSign),
        parseTimestamp(fields[offset + TRADE_TIME_FIELD]));
  }

  private int parseDataCount(String dataCount) {
    try {
      return Integer.parseInt(dataCount);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private BigDecimal parseDecimal(String value) {
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
      log.warn(
          "code={}, message={}, value={}",
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID.getMessage(),
          value);
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal applySign(BigDecimal value, String signCode) {
    if (signCode == null) {
      return value;
    }

    return switch (signCode) {
      case "4", "5", "8", "9" -> value.abs().negate();
      case "3" -> BigDecimal.ZERO;
      default -> value.abs();
    };
  }

  private LocalDateTime parseTimestamp(String kisTime) {
    LocalDate today = LocalDate.now(KOREA_ZONE);
    if (kisTime == null || kisTime.isBlank()) {
      return LocalDateTime.now(KOREA_ZONE).withNano(0);
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
      return LocalDateTime.now(KOREA_ZONE).withNano(0);
    }
  }

  public record ParsedMarketIndex(MarketIndexMarket market, MarketIndexResDTO.MarketIndex index) {}
}
