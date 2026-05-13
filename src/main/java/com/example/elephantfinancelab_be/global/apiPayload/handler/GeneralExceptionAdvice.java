package com.example.elephantfinancelab_be.global.apiPayload.handler;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException ex) {
    log.warn(
        "code={}, message={}, detail={}",
        ex.getCode().getCode(),
        ex.getCode().getMessage(),
        ex.getMessage());
    log.debug("GeneralException detail", ex);
    return ResponseEntity.status(ex.getCode().getStatus())
        .body(ApiResponse.onFailure(ex.getCode(), null));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.error("DB constraint violation: {}", ex.getMostSpecificCause().getMessage());
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.onFailure(code, "데이터 무결성 위반입니다. 입력값을 확인해 주세요."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    Map<String, List<String>> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            err ->
                errors
                    .computeIfAbsent(err.getField(), k -> new ArrayList<>())
                    .add(err.getDefaultMessage()));
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    log.warn("code={}, message={}, detail={}", code.getCode(), code.getMessage(), ex.getMessage());
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentOrState(RuntimeException ex) {
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    log.warn(
        "code={}, message={}, exception={}, detail={}",
        code.getCode(),
        code.getMessage(),
        ex.getClass().getSimpleName(),
        ex.getMessage());
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException ex) {
    BaseErrorCode code = GeneralErrorCode.UNAUTHORIZED;
    log.warn("code={}, message={}, detail={}", code.getCode(), code.getMessage(), ex.getMessage());
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
    BaseErrorCode code = GeneralErrorCode.FORBIDDEN;
    log.warn("code={}, message={}, detail={}", code.getCode(), code.getMessage(), ex.getMessage());
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
    log.error("Unexpected error", ex);
    BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }
}
