package com.example.elephantfinancelab_be.domain.recommendation.converter;

import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecommendationConverter {

  public static RecommendationResponseDTO.RecommendationListDTO toRecommendationListDTO(
      String profile, List<RecommendationResponseDTO.RecommendationInfoDTO> infoList) {
    return RecommendationResponseDTO.RecommendationListDTO.builder()
        .userProfileSummary(profile)
        .recommendations(infoList)
        .build();
  }

  public static RecommendationResponseDTO.RecommendationInfoDTO toRecommendationInfoDTO(
      Recommendation entity) {
    return RecommendationResponseDTO.RecommendationInfoDTO.builder()
        .rank(entity.getRanking())
        .tickerCode(entity.getTickerCode())
        .companyName(entity.getCompanyName())
        .logoUrl(entity.getLogoUrl())
        .currentPrice(entity.getCurrentPrice())
        .changeRate(entity.getChangeRate())
        .currency(entity.getCurrency())
        .isSelected(false)
        .reason(
            entity.getRecommendReason() == null
                ? ""
                : entity.getRecommendReason().length() > 10
                    ? entity.getRecommendReason().substring(0, 10) + "..."
                    : entity.getRecommendReason())
        .score(entity.getScore())
        .build();
  }

  public static RecommendationResponseDTO.RecommendationDetailDTO toRecommendationDetailDTO(
      Recommendation entity, String profile) {
    return RecommendationResponseDTO.RecommendationDetailDTO.builder()
        .recommendationId(entity.getId())
        .tickerCode(entity.getTickerCode())
        .companyName(entity.getCompanyName())
        .logoUrl(entity.getLogoUrl())
        .userProfileSummary(profile)
        .currentPrice(entity.getCurrentPrice())
        .changeRate(entity.getChangeRate())
        .currency(entity.getCurrency())
        .rank(entity.getRanking())
        .score(entity.getScore())
        .sections(
            RecommendationResponseDTO.DetailSectionsDTO.builder()
                .recommendReason(entity.getRecommendReason())
                .companySummary(entity.getCompanySummary())
                .growthPoint(entity.getGrowthPoint())
                .priceAttractiveness(entity.getPriceAttractiveness())
                .risk(entity.getRisk())
                .build())
        .build();
  }
}
