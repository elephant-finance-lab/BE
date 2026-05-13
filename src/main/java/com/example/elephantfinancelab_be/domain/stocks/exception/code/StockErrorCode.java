package com.example.elephantfinancelab_be.domain.stocks.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements BaseErrorCode {
  STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK404_1", "존재하지 않는 종목입니다."),
  STOCK_SUMMARY_CACHE_SERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_1", "종목 요약 캐시 저장에 실패했습니다."),
  STOCK_SUMMARY_CACHE_DESERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "STOCK500_2", "종목 요약 캐시 조회에 실패했습니다."),
  KIS_STOCK_PRICE_API_FAILED(HttpStatus.BAD_GATEWAY, "STOCK502_1", "한국투자증권 현재가 조회에 실패했습니다."),
  KIS_STOCK_PRICE_WEBSOCKET_FAILED(
      HttpStatus.BAD_GATEWAY, "STOCK502_2", "한국투자증권 실시간 체결가 구독에 실패했습니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
