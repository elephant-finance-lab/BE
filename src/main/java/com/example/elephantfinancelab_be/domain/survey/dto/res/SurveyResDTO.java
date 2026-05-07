package com.example.elephantfinancelab_be.domain.survey.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SurveyResDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "선호도 조사 응답 제출 결과")
  public static class SubmitResponse {

    @Schema(description = "저장된 설문 응답 ID", example = "1")
    private Long surveyResponseId;

    @Schema(description = "설문 응답 저장 일시", example = "2026-05-05T10:30:00")
    private LocalDateTime createdAt;
  }
}
