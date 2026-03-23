package com.example.elephantfinancelab_be.global.apiPayload.code;

import org.springframework.http.HttpStatus;

public interface BaseSuccessCode {

  HttpStatus getStatus();

  String getCode();

  String getMessage();
}
