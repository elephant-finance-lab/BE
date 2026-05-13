package com.example.elephantfinancelab_be.domain.stocks.entity;

public enum StockPriceDirection {
  UP,
  FLAT,
  DOWN,
  UNKNOWN;

  public static StockPriceDirection fromSignCode(String signCode) {
    return switch (signCode) {
      case "1", "2" -> UP;
      case "3" -> FLAT;
      case "4", "5" -> DOWN;
      default -> UNKNOWN;
    };
  }
}
