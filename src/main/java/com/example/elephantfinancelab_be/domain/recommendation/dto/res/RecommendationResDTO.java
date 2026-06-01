package com.example.elephantfinancelab_be.domain.recommendation.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
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
  @Schema(name = "RecommendationListResponse", description = "추천 종목 목록 응답")
  public static class RecommendationListDTO {
    private String userProfileSummary;
    private String modelStatus;
    private String modelReason;
    private String generatedAt;
    private String bundleId;
    private String modelVersion;
    private String asof;
    private String mode;
    private Long cacheAgeSec;
    private Boolean stale;
    private String staleReason;
    private Boolean advisoryOnly;
    private Boolean safeToEnableOrderActions;
    private Boolean liveTradingAllowed;
    private List<RecommendationInfoDTO> recommendations;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RecommendationInfo", description = "추천 종목 요약 정보")
  public static class RecommendationInfoDTO {
    private Long recommendationId;
    private String modelRecommendationId;
    private Integer rank;
    private String tickerCode;
    private String stockCode;
    private String companyName;
    private String stockName;
    private String logoUrl;
    private Long currentPrice;
    private Double changeRate;
    private String currency;
    private Boolean isSelected;
    private String reason;
    private Double score;
    private Double expectedReturn;
    private Boolean expectedReturnAvailable;
    private String riskLevel;
    private String modelVersion;
    private String bundleId;
  }

  // --- 추천 종목 상세 조회 응답 ---
  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RecommendationDetailResponse", description = "추천 종목 상세 응답")
  public static class RecommendationDetailDTO {
    private Long recommendationId;
    private String modelRecommendationId;
    private String tickerCode;
    private String stockCode;
    private String companyName;
    private String stockName;
    private String logoUrl;
    private String userProfileSummary;
    private DetailSectionsDTO sections;
    private String recommendReason;
    private String companySummary;
    private String growthPoint;
    private String priceAttractiveness;
    private String risk;
    private Long currentPrice;
    private Double changeRate;
    private String currency;
    private Integer rank;
    private Double score;
    private Double expectedReturn;
    private Boolean expectedReturnAvailable;
    private String riskLevel;
    private String modelVersion;
    private String bundleId;
    private String modelGeneratedAt;
    private String modelAsof;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RecommendationDetailSections", description = "추천 상세 섹션")
  public static class DetailSectionsDTO {
    private String recommendReason;
    private String companySummary;
    private String growthPoint;
    private String priceAttractiveness;
    private String risk;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(name = "RecommendationSelectResponse", description = "추천 종목 선택 저장 응답")
  public static class RecommendationSelectDTO {
    private int selectedCount;
    private List<Long> recommendationIds;
    private List<String> stockCodes;
  }
}
