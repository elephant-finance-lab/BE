package com.example.elephantfinancelab_be.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  TOKEN_MISSING_OR_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_01", "토큰이 없거나 만료되었습니다."),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH401_02", "유효하지 않은 토큰입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
