package com.example.elephantfinancelab_be.domain.stocks.dto.res;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockResDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Summary {

    private String stockName;
    private String ticker;
    private Long currentPriceKrw;
    private Long changeAmountKrw;
    private BigDecimal changeRate;
    private String signCode;
    private StockPriceDirection direction;
    private LocalDateTime updatedAt;
  }
}
