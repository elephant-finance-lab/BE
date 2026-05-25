package com.example.elephantfinancelab_be.domain.autotrading.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AutoTradingErrorCode implements BaseErrorCode {
  IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "AUTO_TRADING400_01", "Idempotency-Key가 필요합니다."),
  INVALID_RECOMMENDATIONS(HttpStatus.BAD_REQUEST, "AUTO_TRADING400_02", "선택한 추천 종목이 유효하지 않습니다."),
  SELECTED_TICKERS_EMPTY(HttpStatus.BAD_REQUEST, "AUTO_TRADING400_03", "자동매매를 시작할 종목이 없습니다."),
  INVALID_PURCHASE_OPTION(HttpStatus.BAD_REQUEST, "AUTO_TRADING400_04", "매수 비중 옵션이 유효하지 않습니다."),
  SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTO_TRADING404_01", "자동매매 세션을 찾을 수 없습니다."),
  ACTIVE_SESSION_EXISTS(
      HttpStatus.CONFLICT, "AUTO_TRADING409_01", "공용 모의계좌에서 이미 실행 중인 자동매매 세션이 있습니다."),
  SESSION_NOT_STOPPABLE(HttpStatus.CONFLICT, "AUTO_TRADING409_02", "현재 상태에서는 자동매매 세션을 중지할 수 없습니다."),
  AI_REQUEST_ID_NOT_FOUND(HttpStatus.CONFLICT, "AUTO_TRADING409_03", "AI 상태 조회에 필요한 요청 ID가 없습니다."),
  AI_START_REJECTED(HttpStatus.BAD_GATEWAY, "AUTO_TRADING502_01", "AI 서버가 자동매매 시작 요청을 거부했습니다."),
  AI_STOP_REJECTED(HttpStatus.BAD_GATEWAY, "AUTO_TRADING502_02", "AI 서버가 자동매매 중지 요청을 거부했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
