package com.example.elephantfinancelab_be.domain.stocks.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StockPriceDirectionTest {

  @Test
  void fromSignCodeReturnsUnknownWhenSignCodeIsNull() {
    assertThat(StockPriceDirection.fromSignCode(null)).isEqualTo(StockPriceDirection.UNKNOWN);
  }
}
