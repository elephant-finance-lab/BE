package com.example.elephantfinancelab_be.domain.stocks.dto.res;

import java.math.BigDecimal;
import java.util.List;

public class StockDailyPriceResDTO {

  public record DailyPrices(String ticker, List<Item> items) {}

  public record Item(
      String date, Long closePrice, BigDecimal changeRate, Long volume, Long tradingValue) {}
}
