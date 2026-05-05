package com.example.elephantfinancelab_be.domain.chart.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarketIndexRealtimeParserTest {

  private final MarketIndexRealtimeParser parser = new MarketIndexRealtimeParser();

  @Test
  void parseKisRealtimeIndexMessage() {
    String message = "0|H0UPCNT0|001|0001^143000^2735.24^2^12.31^0^0^0^0^0.45";

    MarketIndexRealtimeParser.ParsedMarketIndex result = parser.parse(message).orElseThrow();

    assertThat(result.market()).isEqualTo(MarketIndexMarket.KOSPI);
    assertThat(result.index().market()).isEqualTo("KOSPI");
    assertThat(result.index().value()).isEqualByComparingTo(new BigDecimal("2735.24"));
    assertThat(result.index().change()).isEqualByComparingTo(new BigDecimal("12.31"));
    assertThat(result.index().changeRate()).isEqualByComparingTo(new BigDecimal("0.45"));
  }

  @Test
  void parseKisRealtimeIndexMessageWithNegativeSign() {
    String message = "0|H0UPCNT0|001|1001^143000^862.15^5^3.18^0^0^0^0^0.37";

    MarketIndexRealtimeParser.ParsedMarketIndex result = parser.parse(message).orElseThrow();

    assertThat(result.market()).isEqualTo(MarketIndexMarket.KOSDAQ);
    assertThat(result.index().change()).isEqualByComparingTo(new BigDecimal("-3.18"));
    assertThat(result.index().changeRate()).isEqualByComparingTo(new BigDecimal("-0.37"));
  }

  @Test
  void parseMultipleKisRealtimeIndexMessages() {
    String message =
        "0|H0UPCNT0|002|"
            + "0001^143000^2735.24^2^12.31^0^0^0^0^0.45^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0"
            + "^1001^143000^862.15^5^3.18^0^0^0^0^0.37^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0^0";

    var result = parser.parseAll(message);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).market()).isEqualTo(MarketIndexMarket.KOSPI);
    assertThat(result.get(1).market()).isEqualTo(MarketIndexMarket.KOSDAQ);
  }
}
