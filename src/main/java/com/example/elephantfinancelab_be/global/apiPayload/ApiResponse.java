package com.example.elephantfinancelab_be.global.apiPayload;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.BaseSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

  @JsonProperty("isSuccess")
  private final boolean isSuccess;

  @JsonProperty("code")
  private final String code;

  @JsonProperty("message")
  private final String message;

  @JsonProperty("result")
  private T result;

  public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
    return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
  }

  // 데이터가 있는 성공 응답
  public static <T> ApiResponse<T> of(BaseSuccessCode code, T result) {
    return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
  }

  // 데이터가 없는 성공 응답
  public static <T> ApiResponse<T> of(BaseSuccessCode code) {
    return new ApiResponse<>(true, code.getCode(), code.getMessage(), null);
  }
}
