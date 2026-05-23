package com.example.elephantfinancelab_be.domain.stocks.dto.res;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import java.util.List;

public class StockInfoResDTO {

  public record Info(
      String ticker, String nameKor, Price price, FinancialSummary financialSummary) {}

  public record Price(
      Long dayLowPriceKrw,
      Long dayHighPriceKrw,
      Long yearLowPriceKrw,
      Long yearHighPriceKrw,
      Long openPriceKrw,
      Long closePriceKrw) {}

  public record FinancialSummary(
      StockFinancialPeriod period, String unit, List<String> columns, List<Row> rows) {}

  public record Row(String label, List<String> values) {}
}
