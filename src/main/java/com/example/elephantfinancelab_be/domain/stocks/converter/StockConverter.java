package com.example.elephantfinancelab_be.domain.stocks.converter;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockConverter {

  private StockConverter() {}

  public static StockResDTO.Summary toSummary(
      Stock stock,
      Long currentPriceKrw,
      Long changeAmountKrw,
      BigDecimal changeRate,
      String signCode,
      LocalDateTime updatedAt) {
    return toSummary(
        stock.getName(),
        stock.getTicker(),
        currentPriceKrw,
        changeAmountKrw,
        changeRate,
        signCode,
        updatedAt);
  }

  public static StockResDTO.Summary toSummary(
      String stockName,
      String ticker,
      Long currentPriceKrw,
      Long changeAmountKrw,
      BigDecimal changeRate,
      String signCode,
      LocalDateTime updatedAt) {
    return StockResDTO.Summary.builder()
        .stockName(stockName)
        .ticker(ticker)
        .currentPriceKrw(currentPriceKrw)
        .changeAmountKrw(changeAmountKrw)
        .changeRate(changeRate)
        .signCode(signCode)
        .direction(StockPriceDirection.fromSignCode(signCode))
        .updatedAt(updatedAt)
        .build();
  }
}
