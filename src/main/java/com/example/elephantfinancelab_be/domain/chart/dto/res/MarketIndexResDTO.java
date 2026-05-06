package com.example.elephantfinancelab_be.domain.chart.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MarketIndexResDTO {

  public record MarketIndex(
      String market,
      BigDecimal value,
      BigDecimal change,
      BigDecimal changeRate,
      LocalDateTime timestamp) {}

  public record MarketIndexes(MarketIndex kospi, MarketIndex kosdaq) {}
}
