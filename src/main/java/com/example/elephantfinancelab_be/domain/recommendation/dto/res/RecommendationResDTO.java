package com.example.elephantfinancelab_be.domain.recommendation.dto.res;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecommendationResDTO {

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecommendationListDTO {
    private String userProfileSummary;
    private List<RecommendationInfoDTO> recommendations;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecommendationInfoDTO {
    private Integer rank;
    private String tickerCode;
    private String companyName;
    private String logoUrl;
    private Long currentPrice;
    private Double changeRate;
    private String currency;
    private Boolean isSelected;
    private String reason;
    private Double score;
  }

  // --- 추천 종목 상세 조회 응답 ---
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecommendationDetailDTO {
    private Long recommendationId;
    private String tickerCode;
    private String companyName;
    private String logoUrl;
    private String userProfileSummary;
    private DetailSectionsDTO sections;
    private Long currentPrice;
    private Double changeRate;
    private String currency;
    private Integer rank;
    private Double score;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailSectionsDTO {
    private String recommendReason;
    private String companySummary;
    private String growthPoint;
    private String priceAttractiveness;
    private String risk;
  }

  // --- 추천 종목 선택 저장 응답 ---
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecommendationSelectDTO {
    private int selectedCount;
    private List<Long> recommendationIds;
  }

  // --- 매수 비중 옵션 저장 응답 ---
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PurchaseOptionDTO {
    private int optionId;
    private int minRate;
    private int maxRate;
    private String label;
  }
}
