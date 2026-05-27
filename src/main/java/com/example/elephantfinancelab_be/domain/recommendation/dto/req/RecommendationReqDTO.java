package com.example.elephantfinancelab_be.domain.recommendation.dto.req;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecommendationReqDTO {

  @Getter
  @NoArgsConstructor
  @Schema(
      name = "SelectRecommendationDTO",
      description =
          "추천 종목 선택 저장 요청. 단건 저장은 recommendationId/stockCode를, 다건 저장은 selectedRecommendations를 사용합니다.")
  public static class SelectRecommendationDTO {
    @Schema(description = "단건 선택용 추천 ID", example = "1")
    @Positive
    private Long recommendationId;

    @Schema(description = "단건 선택용 종목 코드", example = "005930")
    @Size(max = 20)
    private String stockCode;

    @ArraySchema(
        schema =
            @Schema(
                implementation = RecommendationIdDTO.class,
                description = "다건 선택용 추천 종목 식별자 목록"))
    @Valid
    @Size(max = 50)
    private List<@NotNull @Valid RecommendationIdDTO> selectedRecommendations;

    @AssertTrue(message = "단건 선택과 다건 선택 중 하나만 입력해야 합니다.")
    public boolean isSelectionModeValid() {
      boolean hasSingle = recommendationId != null || hasText(stockCode);
      boolean hasMultiple = selectedRecommendations != null && !selectedRecommendations.isEmpty();
      return hasSingle ^ hasMultiple;
    }
  }

  @Getter
  @NoArgsConstructor
  @Schema(name = "RecommendationSelectionItem", description = "추천 종목 선택 항목")
  public static class RecommendationIdDTO {
    @Schema(description = "추천 ID", example = "1")
    @Positive
    private Long recommendationId;

    @Schema(description = "종목 코드", example = "005930")
    @Size(max = 20)
    private String stockCode;

    @AssertTrue(message = "recommendationId 또는 stockCode 중 하나는 필요합니다.")
    public boolean isIdentifierPresent() {
      return recommendationId != null || hasText(stockCode);
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
