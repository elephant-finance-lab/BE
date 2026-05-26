package com.example.elephantfinancelab_be.domain.recommendation.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecommendationErrorCode implements BaseErrorCode {
  NO_SELECTED_RECOMMENDATION(HttpStatus.BAD_REQUEST, "RECOMMENDATION400_01", "선택한 추천 종목이 없습니다."),
  RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION404_01", "추천 종목을 찾을 수 없습니다."),
  MODEL_RECOMMENDATION_UNAVAILABLE(
      HttpStatus.SERVICE_UNAVAILABLE, "RECOMMENDATION503_01", "AI 모델 추천 결과를 사용할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
