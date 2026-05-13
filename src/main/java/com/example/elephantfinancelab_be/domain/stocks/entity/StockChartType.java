package com.example.elephantfinancelab_be.domain.stocks.entity;

import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.util.Arrays;

public enum StockChartType {
  LINE,
  CANDLE;

  public static StockChartType from(String value) {
    if (value == null || value.isBlank()) {
      throw new StockException(StockErrorCode.INVALID_STOCK_CHART_TYPE);
    }

    return Arrays.stream(values())
        .filter(type -> type.name().equalsIgnoreCase(value.trim()))
        .findFirst()
        .orElseThrow(() -> new StockException(StockErrorCode.INVALID_STOCK_CHART_TYPE));
  }
}
