package com.example.elephantfinancelab_be.domain.recommendation.dto.req;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
    private Long recommendationId;

    @Schema(description = "단건 선택용 종목 코드", example = "005930")
    private String stockCode;

    @ArraySchema(
        schema =
            @Schema(
                implementation = RecommendationIdDTO.class,
                description = "다건 선택용 추천 종목 식별자 목록"))
    @Valid
    private List<RecommendationIdDTO> selectedRecommendations;
  }

  @Getter
  @NoArgsConstructor
  @Schema(name = "RecommendationSelectionItem", description = "추천 종목 선택 항목")
  public static class RecommendationIdDTO {
    @Schema(description = "추천 ID", example = "1")
    private Long recommendationId;

    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;
  }
}
