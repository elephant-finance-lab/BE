package com.example.elephantfinancelab_be.global.apiPayload.handler;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import java.util.HashMap;
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
    log.warn("Business exception [{}]: {}", ex.getCode().getCode(), ex.getCode().getMessage());
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
  public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    log.warn("Malformed request body: {}", ex.getMessage());
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<ApiResponse<String>> handleIllegalArgumentOrState(RuntimeException ex) {
    log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
    String detail = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : null;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, detail));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
      AuthenticationException ex) {
    log.warn("Authentication failed: {}", ex.getMessage());
    BaseErrorCode code = GeneralErrorCode.UNAUTHORIZED;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    BaseErrorCode code = GeneralErrorCode.FORBIDDEN;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
    log.error("Unexpected error", ex);
    BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
  }
}
