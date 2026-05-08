package com.example.elephantfinancelab_be.domain.recommendation.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecommendationErrorCode implements BaseErrorCode {
  NO_SELECTED_RECOMMENDATION(HttpStatus.BAD_REQUEST, "RECOMMENDATION400_01", "선택한 추천 종목이 없습니다."),
  NO_PRIOR_SELECTION(HttpStatus.CONFLICT, "RECOMMENDATION409_02", "선택된 추천 종목 정보가 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
