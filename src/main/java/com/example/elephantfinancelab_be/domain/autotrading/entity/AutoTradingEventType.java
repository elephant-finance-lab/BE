package com.example.elephantfinancelab_be.domain.autotrading.entity;

public enum AutoTradingEventType {
  AUTO_TRADING_STARTED,
  DECISION_COMPLETED,
  PAPER_ORDER_SUBMITTED,
  PAPER_ORDER_FILLED,
  PAPER_ORDER_FAILED,
  AUTO_TRADING_STOPPED,
  AUTO_TRADING_FAILED,
  UNKNOWN;

  public static AutoTradingEventType from(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    try {
      return valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return UNKNOWN;
    }
  }

  public boolean isOrderEvent() {
    return this == PAPER_ORDER_SUBMITTED
        || this == PAPER_ORDER_FILLED
        || this == PAPER_ORDER_FAILED;
  }
}
