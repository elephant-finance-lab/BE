package com.example.elephantfinancelab_be.domain.recommendation.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecommendationRequestDTO {

  @Getter
  @NoArgsConstructor
  public static class SelectRecommendationDTO {
    private List<RecommendationIdDTO> selectedRecommendations;
  }

  @Getter
  @NoArgsConstructor
  public static class RecommendationIdDTO {
    private Long recommendationId;
  }

  @Getter
  @NoArgsConstructor
  public static class PurchaseOptionRequestDTO {
    private int optionId;
  }
}
