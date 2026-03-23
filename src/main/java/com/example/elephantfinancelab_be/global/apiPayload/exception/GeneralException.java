package com.example.elephantfinancelab_be.global.apiPayload.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

  private final BaseErrorCode code;
}
