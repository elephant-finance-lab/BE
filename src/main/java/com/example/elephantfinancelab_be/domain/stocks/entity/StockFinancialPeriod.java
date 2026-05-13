package com.example.elephantfinancelab_be.domain.stocks.entity;

import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockFinancialPeriod {
  QUARTER("1"),
  YEAR("0");

  private final String kisDivClsCode;

  public static StockFinancialPeriod from(String value) {
    if (value == null || value.isBlank()) {
      return QUARTER;
    }

    try {
      return StockFinancialPeriod.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new StockException(StockErrorCode.INVALID_STOCK_FINANCIAL_PERIOD);
    }
  }
}
