package com.example.elephantfinancelab_be.domain.chart.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChartErrorCode implements BaseErrorCode {
  INVALID_RANKING_TYPE(HttpStatus.BAD_REQUEST, "CHART400_1", "지원하지 않는 랭킹 타입입니다."),
  RANKING_CACHE_SERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "CHART500_1", "종목 랭킹 캐시 저장에 실패했습니다."),
  MARKET_INDEX_CACHE_SERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "CHART500_2", "시장 지수 캐시 저장에 실패했습니다."),
  MARKET_INDEX_CACHE_DESERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "CHART500_3", "시장 지수 캐시 조회에 실패했습니다."),
  RANKING_CACHE_DESERIALIZE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "CHART500_4", "종목 랭킹 캐시 조회에 실패했습니다."),
  KIS_ACCESS_TOKEN_FAILED(HttpStatus.BAD_GATEWAY, "CHART502_1", "한국투자증권 접근 토큰 발급에 실패했습니다."),
  KIS_RANKING_API_FAILED(HttpStatus.BAD_GATEWAY, "CHART502_2", "한국투자증권 종목 랭킹 조회에 실패했습니다."),
  KIS_APPROVAL_KEY_FAILED(HttpStatus.BAD_GATEWAY, "CHART502_3", "한국투자증권 웹소켓 접속키 발급에 실패했습니다."),
  KIS_RANKING_RESPONSE_PARSE_FAILED(
      HttpStatus.BAD_GATEWAY, "CHART502_4", "한국투자증권 종목 랭킹 응답 파싱에 실패했습니다."),
  KIS_MARKET_INDEX_REALTIME_MESSAGE_INVALID(
      HttpStatus.BAD_GATEWAY, "CHART502_5", "한국투자증권 실시간 시장 지수 메시지가 올바르지 않습니다."),
  KIS_MARKET_INDEX_WEBSOCKET_FAILED(
      HttpStatus.BAD_GATEWAY, "CHART502_6", "한국투자증권 시장 지수 웹소켓 처리에 실패했습니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
