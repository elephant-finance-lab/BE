package com.example.elephantfinancelab_be.domain.stocks.dto.res;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartInterval;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import java.util.List;

public class StockChartResDTO {

  public record Chart(
      String ticker,
      String range,
      StockChartType type,
      StockChartInterval interval,
      String currency,
      List<DataPoint> data) {}

  public record DataPoint(
      String time, Long price, Long open, Long high, Long low, Long close, Long volume) {}
}
