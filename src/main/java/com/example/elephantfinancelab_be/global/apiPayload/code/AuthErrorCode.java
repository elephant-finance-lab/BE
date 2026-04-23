package com.example.elephantfinancelab_be.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  REFRESH_TOKEN_VALID(HttpStatus.OK, "AUTH200_01", "리프레시 토큰이 유효합니다."),
  TOKEN_MISSING_OR_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_01", "토큰이 없거나 만료되었습니다."),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH401_02", "유효하지 않은 토큰입니다."),
  TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "AUTH401_03", "인증 헤더에 토큰이 없습니다."),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_04", "토큰이 만료되었습니다."),
  REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_05", "리프레시 토큰이 만료되었습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH403_01", "접근 권한이 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
