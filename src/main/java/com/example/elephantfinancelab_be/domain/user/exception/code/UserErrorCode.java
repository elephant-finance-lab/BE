package com.example.elephantfinancelab_be.domain.user.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "사용자를 찾을 수 없습니다."),
  USER_ALREADY_DELETED(HttpStatus.GONE, "USER410_1", "이미 탈퇴한 회원입니다."),
  DUPLICATE_PHONE(HttpStatus.CONFLICT, "USER409_1", "이미 사용 중인 전화번호입니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
