package com.example.elephantfinancelab_be.domain.user.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AccountErrorCode implements BaseErrorCode {
  ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT404_01", "존재하지 않는 계좌입니다."),
  ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "ACCOUNT409_01", "이미 등록된 계좌입니다."),
  ACCOUNT_REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "ACCOUNT400_01", "필수 입력값이 누락되었습니다."),
  ACCOUNT_INVALID(HttpStatus.BAD_REQUEST, "ACCOUNT400_02", "유효하지 않은 계좌입니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
