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
    private Long totalAssetAmount;
    private Long stockEvaluationAmount;
    private Long cashAmount;
    private Long totalProfitLossAmount;
    private Double totalProfitLossRate;
    private List<PositionSummary> holdings;
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
    private String stockCode;
    private String stockName;
    private Long averagePrice;
    private Long currentPrice;
    private Long evaluationAmount;
    private Long profitLossAmount;
    private Double profitLossRate;
    private Double weightRate;
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
    private List<TradeDetail> items;
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
    private String tradeDate;
    private TradeType side;
    private String stockCode;
    private String stockName;
    private Long amount;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class HoldingAiDetail {
    private String tickerCode;
    private String companyName;
    private Double aiHitRate;
    private String tradeReason;
    private String futureStrategy;
    private LocalDateTime generatedAt;
  }
}
