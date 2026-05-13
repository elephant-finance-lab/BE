package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
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
public class StockPriceRealtimeParser {

  public static final String DOMESTIC_STOCK_REALTIME_TR_ID = "H0STCNT0";

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter KIS_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
  private static final Duration FUTURE_TIMESTAMP_TOLERANCE = Duration.ofHours(1);
  private static final int RESPONSE_FIELD_COUNT = 46;
  private static final int TICKER_FIELD = 0;
  private static final int TRADE_TIME_FIELD = 1;
  private static final int CURRENT_PRICE_FIELD = 2;
  private static final int CHANGE_SIGN_FIELD = 3;
  private static final int CHANGE_AMOUNT_FIELD = 4;
  private static final int CHANGE_RATE_FIELD = 5;
  private static final int TRADE_VOLUME_FIELD = 12;
  private static final int ACCUMULATED_VOLUME_FIELD = 13;
  private static final int BUSINESS_DATE_FIELD = 33;

  public Optional<ParsedStockPrice> parse(String message) {
    return parseAll(message).stream().findFirst();
  }

  public List<ParsedStockPrice> parseAll(String message) {
    if (message == null || message.isBlank() || message.startsWith("{")) {
      return List.of();
    }

    String[] parts = message.split("\\|", 4);
    if (parts.length < 4 || !DOMESTIC_STOCK_REALTIME_TR_ID.equals(parts[1])) {
      return List.of();
    }

    if ("1".equals(parts[0])) {
      log.warn(
          "code={}, message={}, reason=encrypted",
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getMessage());
      return List.of();
    }

    String[] fields = parts[3].split("\\^", -1);
    if (fields.length <= CHANGE_RATE_FIELD) {
      log.warn(
          "code={}, message={}, count={}",
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getMessage(),
          fields.length);
      return List.of();
    }

    int dataCount = parseDataCount(parts[2]);
    List<ParsedStockPrice> prices = new ArrayList<>();
    for (int index = 0; index < dataCount; index++) {
      int offset = index * RESPONSE_FIELD_COUNT;
      if (fields.length < offset + RESPONSE_FIELD_COUNT) {
        log.warn(
            "code={}, message={}, item={}, count={}, expectedAtLeast={}",
            StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getCode(),
            StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getMessage(),
            index,
            fields.length,
            offset + RESPONSE_FIELD_COUNT);
        break;
      }
      prices.add(toParsedStockPrice(fields, offset));
    }

    return prices;
  }

  private ParsedStockPrice toParsedStockPrice(String[] fields, int offset) {
    String signCode = fields[offset + CHANGE_SIGN_FIELD];
    return new ParsedStockPrice(
        fields[offset + TICKER_FIELD],
        parseDecimal(fields[offset + CURRENT_PRICE_FIELD]).abs().longValue(),
        applySign(parseDecimal(fields[offset + CHANGE_AMOUNT_FIELD]), signCode).longValue(),
        applySign(parseDecimal(fields[offset + CHANGE_RATE_FIELD]), signCode),
        signCode,
        StockPriceDirection.fromSignCode(signCode),
        parseDecimal(fieldValue(fields, offset + TRADE_VOLUME_FIELD)).abs().longValue(),
        parseDecimal(fieldValue(fields, offset + ACCUMULATED_VOLUME_FIELD)).abs().longValue(),
        parseTimestamp(
            fieldValue(fields, offset + BUSINESS_DATE_FIELD),
            fieldValue(fields, offset + TRADE_TIME_FIELD)));
  }

  private int parseDataCount(String dataCount) {
    try {
      return Integer.parseInt(dataCount);
    } catch (NumberFormatException e) {
      return 1;
    }
  }

  private String fieldValue(String[] fields, int index) {
    if (index >= fields.length) {
      return null;
    }
    return fields[index];
  }

  private BigDecimal parseDecimal(String value) {
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value.trim().replace(",", ""));
    } catch (NumberFormatException e) {
      log.warn(
          "code={}, message={}, value={}",
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID.getMessage(),
          value);
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal applySign(BigDecimal value, String signCode) {
    StockPriceDirection direction = StockPriceDirection.fromSignCode(signCode);
    // FLAT sign means no price movement, so ignore any non-zero raw delta from KIS.
    return switch (direction) {
      case DOWN -> value.abs().negate();
      case FLAT -> BigDecimal.ZERO;
      case UP, UNKNOWN -> value.abs();
    };
  }

  private LocalDateTime parseTimestamp(String kisDate, String kisTime) {
    LocalDate date = parseDate(kisDate);
    if (kisTime == null || kisTime.isBlank()) {
      return LocalDateTime.now(KOREA_ZONE).withNano(0);
    }

    try {
      LocalDateTime now = LocalDateTime.now(KOREA_ZONE).withNano(0);
      LocalDateTime timestamp =
          LocalDateTime.of(date, LocalTime.parse(kisTime, KIS_TIME_FORMATTER));
      if (timestamp.isAfter(now.plus(FUTURE_TIMESTAMP_TOLERANCE))) {
        return timestamp.minusDays(1);
      }
      return timestamp;
    } catch (DateTimeParseException e) {
      return LocalDateTime.now(KOREA_ZONE).withNano(0);
    }
  }

  private LocalDate parseDate(String kisDate) {
    if (kisDate == null || kisDate.isBlank()) {
      return LocalDate.now(KOREA_ZONE);
    }

    try {
      return LocalDate.parse(kisDate, KIS_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      return LocalDate.now(KOREA_ZONE);
    }
  }

  public record ParsedStockPrice(
      String ticker,
      Long currentPriceKrw,
      Long changeAmountKrw,
      BigDecimal changeRate,
      String signCode,
      StockPriceDirection direction,
      Long tradeVolume,
      Long accumulatedVolume,
      LocalDateTime updatedAt) {}
}
