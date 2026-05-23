package com.example.elephantfinancelab_be.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiServerErrorCode implements BaseErrorCode {
  AI503_01(HttpStatus.SERVICE_UNAVAILABLE, "AI503_01", "AI 서버에 연결할 수 없습니다."),
  AI504_01(HttpStatus.GATEWAY_TIMEOUT, "AI504_01", "AI 서버 응답 시간이 초과되었습니다."),
  AI400_01(HttpStatus.BAD_REQUEST, "AI400_01", "AI 서버 요청이 잘못되었습니다."),
  AI500_01(HttpStatus.INTERNAL_SERVER_ERROR, "AI500_01", "AI 서버 내부 오류가 발생했습니다."),
  AI401_01(HttpStatus.UNAUTHORIZED, "AI401_01", "AI 서버 인증에 실패했습니다."),
  AI403_01(HttpStatus.FORBIDDEN, "AI403_01", "AI 서버 접근이 거부되었습니다."),
  AI404_01(HttpStatus.NOT_FOUND, "AI404_01", "AI 서버에서 요청한 리소스를 찾을 수 없습니다."),
  AI429_01(HttpStatus.TOO_MANY_REQUESTS, "AI429_01", "AI 서버 요청이 너무 많습니다."),
  AI501_01(HttpStatus.NOT_IMPLEMENTED, "AI501_01", "AI 서버에서 지원하지 않는 기능입니다."),
  AI400_02(HttpStatus.BAD_REQUEST, "AI400_02", "AI 서버 요청이 취소되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
