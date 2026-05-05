package com.example.elephantfinancelab_be.domain.recommendation.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecommendationReqDTO {

  @Getter
  @NoArgsConstructor
  public static class SelectRecommendationDTO {
    @NotEmpty @Valid private List<RecommendationIdDTO> selectedRecommendations;
  }

  @Getter
  @NoArgsConstructor
  public static class RecommendationIdDTO {
    @NotNull private Long recommendationId;
  }

  @Getter
  @NoArgsConstructor
  public static class PurchaseOptionRequestDTO {
    @Min(1)
    @Max(4)
    private int optionId;
  }
}
