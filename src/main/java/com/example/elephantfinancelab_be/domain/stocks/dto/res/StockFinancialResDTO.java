package com.example.elephantfinancelab_be.domain.stocks.dto.res;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import java.util.List;

public class StockFinancialResDTO {

  public record Financial(
      String ticker,
      String nameKor,
      StockFinancialStatement statement,
      StockFinancialPeriod period,
      String unit,
      List<String> columns,
      List<Row> rows) {}

  public record Row(String label, List<String> values) {}
}
