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
  AI_DETAIL404_01(HttpStatus.NOT_FOUND, "AI_DETAIL404_01", "AI 상세 정보가 존재하지 않습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
