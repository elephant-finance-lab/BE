package com.example.elephantfinancelab_be.domain.watchlist.exception;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;

public class WatchlistException extends GeneralException {
  public WatchlistException(BaseErrorCode code) {
    super(code);
  }
}
