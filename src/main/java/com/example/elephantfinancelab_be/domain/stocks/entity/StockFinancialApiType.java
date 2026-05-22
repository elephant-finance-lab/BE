package com.example.elephantfinancelab_be.domain.stocks.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockFinancialApiType {
  INCOME("/uapi/domestic-stock/v1/finance/income-statement", "FHKST66430200"),
  BALANCE("/uapi/domestic-stock/v1/finance/balance-sheet", "FHKST66430100"),
  FINANCIAL_RATIO("/uapi/domestic-stock/v1/finance/financial-ratio", "FHKST66430300"),
  PROFIT_RATIO("/uapi/domestic-stock/v1/finance/profit-ratio", "FHKST66430400");

  private final String path;
  private final String trId;
}
