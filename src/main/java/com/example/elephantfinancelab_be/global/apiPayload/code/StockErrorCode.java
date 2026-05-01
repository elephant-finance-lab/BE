package com.example.elephantfinancelab_be.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StockErrorCode implements BaseErrorCode {
  STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK404_01", "존재하지 않는 종목입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
