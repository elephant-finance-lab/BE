package com.example.elephantfinancelab_be.domain.watchlist.exception.code;

import com.example.elephantfinancelab_be.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WatchlistErrorCode implements BaseErrorCode {
  WATCHLIST_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST404_1", "관심그룹을 찾을 수 없습니다."),
  WATCHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST404_2", "관심종목을 찾을 수 없습니다."),
  WATCHLIST_GROUP_FORBIDDEN(HttpStatus.FORBIDDEN, "WATCHLIST403_1", "해당 관심그룹에 대한 권한이 없습니다."),
  WATCHLIST_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "WATCHLIST409_1", "이미 추가된 관심종목입니다."),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
