package com.example.elephantfinancelab_be.domain.stocks.entity;

import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockChartRange {
  ONE_DAY("1D", StockChartInterval.MINUTE, Duration.ofSeconds(30), null),
  ONE_WEEK("1W", StockChartInterval.DAY, Duration.ofMinutes(1), "D"),
  THREE_MONTHS("3M", StockChartInterval.DAY, Duration.ofMinutes(5), "D"),
  ONE_YEAR("1Y", StockChartInterval.DAY, Duration.ofMinutes(10), "D"),
  FIVE_YEARS("5Y", StockChartInterval.MONTH, Duration.ofMinutes(30), "M"),
  ALL("ALL", StockChartInterval.YEAR, Duration.ofHours(1), "Y");

  private final String value;
  private final StockChartInterval interval;
  private final Duration cacheTtl;
  private final String kisPeriodDivCode;

  public static StockChartRange from(String value) {
    if (value == null || value.isBlank()) {
      throw new StockException(StockErrorCode.INVALID_STOCK_CHART_RANGE);
    }

    return Arrays.stream(values())
        .filter(range -> range.value.equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(() -> new StockException(StockErrorCode.INVALID_STOCK_CHART_RANGE));
  }

  public LocalDate startDate(LocalDate today) {
    return switch (this) {
      case ONE_DAY -> today;
      case ONE_WEEK -> today.minusWeeks(1);
      case THREE_MONTHS -> today.minusMonths(3);
      case ONE_YEAR -> today.minusYears(1);
      case FIVE_YEARS -> today.minusYears(5);
      case ALL -> LocalDate.of(1900, 1, 1);
    };
  }
}
