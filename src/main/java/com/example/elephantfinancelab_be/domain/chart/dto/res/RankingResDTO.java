package com.example.elephantfinancelab_be.domain.chart.dto.res;

import java.math.BigDecimal;
import java.util.List;

public class RankingResDTO {

  public record RankingResponse(String type, List<RankingItem> items) {}

  public record RankingItem(
      Integer rank,
      String tickerCode,
      String stockName,
      Long price,
      Long change,
      BigDecimal changeRate,
      Long volume,
      BigDecimal metric) {}
}
