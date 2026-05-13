package com.example.elephantfinancelab_be.domain.stocks.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements BaseErrorCode {
  STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK404_01", "존재하지 않는 종목입니다."),
  INVALID_STOCK_CHART_RANGE(HttpStatus.BAD_REQUEST, "STOCK400_1", "지원하지 않는 차트 범위입니다."),
  INVALID_STOCK_CHART_TYPE(HttpStatus.BAD_REQUEST, "STOCK400_2", "지원하지 않는 차트 타입입니다."),
  STOCK_SUMMARY_CACHE_SERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_1", "종목 요약 캐시 저장에 실패했습니다."),
  STOCK_SUMMARY_CACHE_DESERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_2", "종목 요약 캐시 조회에 실패했습니다."),
  STOCK_CHART_CACHE_SERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_3", "종목 차트 캐시 저장에 실패했습니다."),
  STOCK_CHART_CACHE_DESERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_4", "종목 차트 캐시 조회에 실패했습니다."),
  STOCK_SUMMARY_CACHE_SAVE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_5", "종목 요약 캐시 저장에 실패했습니다."),
  STOCK_PRICE_PUSH_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_6", "종목 실시간 체결가 push 전송에 실패했습니다."),
  STOCK_CHART_REALTIME_UPDATE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_7", "종목 차트 실시간 업데이트에 실패했습니다."),
  STOCK_CHART_PUSH_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_8", "종목 차트 실시간 push 전송에 실패했습니다."),
  KIS_STOCK_PRICE_API_FAILED(HttpStatus.BAD_GATEWAY, "STOCK502_1", "한국투자증권 현재가 조회에 실패했습니다."),
  KIS_STOCK_PRICE_WEBSOCKET_FAILED(
      HttpStatus.BAD_GATEWAY, "STOCK502_2", "한국투자증권 실시간 체결가 구독에 실패했습니다."),
  KIS_STOCK_CHART_API_FAILED(HttpStatus.BAD_GATEWAY, "STOCK502_3", "한국투자증권 종목 차트 조회에 실패했습니다."),
  KIS_STOCK_PRICE_RESPONSE_PARSE_FAILED(
      HttpStatus.BAD_GATEWAY, "STOCK502_4", "한국투자증권 현재가 응답 파싱에 실패했습니다."),
  KIS_STOCK_PRICE_REALTIME_MESSAGE_INVALID(
      HttpStatus.BAD_GATEWAY, "STOCK502_5", "한국투자증권 실시간 체결가 메시지가 올바르지 않습니다."),
  KIS_STOCK_CHART_RESPONSE_PARSE_FAILED(
      HttpStatus.BAD_GATEWAY, "STOCK502_6", "한국투자증권 종목 차트 응답 파싱에 실패했습니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
