package com.example.elephantfinancelab_be.domain.portfolio.dto.res;

import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class PortfolioResDTO {

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Summary {
    private Long totalAsset;
    private Long totalProfit;
    private Double totalProfitRate;
    private List<PositionSummary> positions;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class PositionSummary {
    private String tickerCode;
    private String companyName;
    private Integer quantity;
    private Long evalAmount;
    private Double profitRate;
    private Double weight;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class PositionPage {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private List<PositionDetail> positions;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class PositionDetail {
    private Long positionId;
    private String tickerCode;
    private String companyName;
    private Integer quantity;
    private Long avgBuyPrice;
    private Long currentPrice;
    private Long totalBuyAmount;
    private Long evalAmount;
    private Long profitAmount;
    private Double profitRate;
    private LocalDateTime openedAt;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class TradePage {
    private int page;
    private int size;
    private boolean hasNext;
    private List<TradeDetail> trades;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class TradeDetail {
    private Long tradeId;
    private String tickerCode;
    private String companyName;
    private TradeType type;
    private Integer quantity;
    private Long price;
    private Long totalAmount;
    private LocalDateTime tradedAt;
  }
}
