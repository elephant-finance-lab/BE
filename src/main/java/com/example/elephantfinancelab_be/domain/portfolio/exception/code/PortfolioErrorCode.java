package com.example.elephantfinancelab_be.domain.portfolio.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PortfolioErrorCode implements BaseErrorCode {
  PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "PORTFOLIO404_01", "보유 종목이 없습니다."),
  HOLDING404_01(HttpStatus.NOT_FOUND, "HOLDING404_01", "보유 중인 종목 정보를 찾을 수 없습니다."),
  AI_DETAIL404_01(HttpStatus.NOT_FOUND, "AI_DETAIL404_01", "AI 상세 정보가 존재하지 않습니다."),
  TRADE_QUERY_PARAM_CONFLICT(
      HttpStatus.BAD_REQUEST, "PORTFOLIO400_01", "거래 조회의 type과 side가 서로 다릅니다."),
  UNSUPPORTED_TRADE_PERIOD(HttpStatus.BAD_REQUEST, "PORTFOLIO400_02", "지원하지 않는 거래 조회 기간입니다."),
  KIS_ACCOUNT_CONFIG_MISSING(
      HttpStatus.SERVICE_UNAVAILABLE, "PORTFOLIO503_01", "KIS 계좌 환경변수가 설정되어 있지 않습니다."),
  KIS_PORTFOLIO_API_FAILED(HttpStatus.BAD_GATEWAY, "PORTFOLIO502_01", "한국투자증권 포트폴리오 조회에 실패했습니다."),
  KIS_PORTFOLIO_RESPONSE_PARSE_FAILED(
      HttpStatus.BAD_GATEWAY, "PORTFOLIO502_02", "한국투자증권 포트폴리오 응답 파싱에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
