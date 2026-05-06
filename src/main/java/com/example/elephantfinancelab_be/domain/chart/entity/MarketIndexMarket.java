package com.example.elephantfinancelab_be.domain.chart.entity;

import java.util.Arrays;
import java.util.Optional;

public enum MarketIndexMarket {
  KOSPI("0001", "market:index:KOSPI"),
  KOSDAQ("1001", "market:index:KOSDAQ");

  private final String kisIndexCode;
  private final String redisKey;

  MarketIndexMarket(String kisIndexCode, String redisKey) {
    this.kisIndexCode = kisIndexCode;
    this.redisKey = redisKey;
  }

  public String getKisIndexCode() {
    return kisIndexCode;
  }

  public String getRedisKey() {
    return redisKey;
  }

  public static Optional<MarketIndexMarket> fromKisIndexCode(String kisIndexCode) {
    return Arrays.stream(values())
        .filter(market -> market.kisIndexCode.equals(kisIndexCode))
        .findFirst();
  }
}
