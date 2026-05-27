package com.example.elephantfinancelab_be.domain.recommendation.converter;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecommendationConverter {

  public static RecommendationResDTO.RecommendationListDTO toRecommendationListDTO(
      String profile,
      String modelStatus,
      String modelReason,
      String generatedAt,
      String bundleId,
      String modelVersion,
      String asof,
      String mode,
      List<RecommendationResDTO.RecommendationInfoDTO> infoList) {
    return RecommendationResDTO.RecommendationListDTO.builder()
        .userProfileSummary(profile)
        .modelStatus(modelStatus)
        .modelReason(modelReason)
        .generatedAt(generatedAt)
        .bundleId(bundleId)
        .modelVersion(modelVersion)
        .asof(asof)
        .mode(mode)
        .recommendations(infoList)
        .build();
  }

  public static RecommendationResDTO.RecommendationInfoDTO toRecommendationInfoDTO(
      Recommendation entity) {
    return RecommendationResDTO.RecommendationInfoDTO.builder()
        .recommendationId(entity.getId())
        .modelRecommendationId(entity.getModelRecommendationId())
        .rank(entity.getRanking())
        .tickerCode(entity.getTickerCode())
        .stockCode(entity.getTickerCode())
        .companyName(entity.getCompanyName())
        .stockName(entity.getCompanyName())
        .logoUrl(entity.getLogoUrl())
        .currentPrice(entity.getCurrentPrice())
        .changeRate(entity.getChangeRate())
        .currency(entity.getCurrency())
        .isSelected(false)
        .reason(entity.getRecommendReason())
        .score(entity.getScore())
        .expectedReturn(entity.getExpectedReturn())
        .expectedReturnAvailable(entity.getExpectedReturnAvailable())
        .riskLevel(entity.getRiskLevel())
        .modelVersion(entity.getModelVersion())
        .bundleId(entity.getModelBundleId())
        .build();
  }

  public static RecommendationResDTO.RecommendationDetailDTO toRecommendationDetailDTO(
      Recommendation entity, String profile) {
    return RecommendationResDTO.RecommendationDetailDTO.builder()
        .recommendationId(entity.getId())
        .modelRecommendationId(entity.getModelRecommendationId())
        .tickerCode(entity.getTickerCode())
        .stockCode(entity.getTickerCode())
        .companyName(entity.getCompanyName())
        .stockName(entity.getCompanyName())
        .logoUrl(entity.getLogoUrl())
        .userProfileSummary(profile)
        .recommendReason(entity.getRecommendReason())
        .companySummary(entity.getCompanySummary())
        .growthPoint(entity.getGrowthPoint())
        .priceAttractiveness(entity.getPriceAttractiveness())
        .risk(entity.getRisk())
        .currentPrice(entity.getCurrentPrice())
        .changeRate(entity.getChangeRate())
        .currency(entity.getCurrency())
        .rank(entity.getRanking())
        .score(entity.getScore())
        .expectedReturn(entity.getExpectedReturn())
        .expectedReturnAvailable(entity.getExpectedReturnAvailable())
        .riskLevel(entity.getRiskLevel())
        .modelVersion(entity.getModelVersion())
        .bundleId(entity.getModelBundleId())
        .modelGeneratedAt(entity.getModelGeneratedAt())
        .modelAsof(entity.getModelAsof())
        .sections(
            RecommendationResDTO.DetailSectionsDTO.builder()
                .recommendReason(entity.getRecommendReason())
                .companySummary(entity.getCompanySummary())
                .growthPoint(entity.getGrowthPoint())
                .priceAttractiveness(entity.getPriceAttractiveness())
                .risk(entity.getRisk())
                .build())
        .build();
  }
}
