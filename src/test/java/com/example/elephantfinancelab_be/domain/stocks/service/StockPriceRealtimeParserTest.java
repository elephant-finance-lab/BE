package com.example.elephantfinancelab_be.domain.stocks.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockPriceDirection;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockPriceRealtimeParserTest {

  private final StockPriceRealtimeParser parser = new StockPriceRealtimeParser();

  @Test
  void parseKisRealtimeStockPriceMessage() {
    String message =
        "0|H0STCNT0|001|" + item("005930", "093354", "71900", "5", "-100", "-0.14", "20230612");

    StockPriceRealtimeParser.ParsedStockPrice result = parser.parse(message).orElseThrow();

    assertThat(result.ticker()).isEqualTo("005930");
    assertThat(result.currentPriceKrw()).isEqualTo(71900L);
    assertThat(result.changeAmountKrw()).isEqualTo(-100L);
    assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("-0.14"));
    assertThat(result.signCode()).isEqualTo("5");
    assertThat(result.direction()).isEqualTo(StockPriceDirection.DOWN);
    assertThat(result.updatedAt().toLocalDate().toString()).isEqualTo("2023-06-12");
  }

  @Test
  void parseKisRealtimeStockPriceMessageWithUpSign() {
    String message =
        "0|H0STCNT0|001|" + item("000660", "143000", "180000", "2", "2500", "1.41", "20260513");

    StockPriceRealtimeParser.ParsedStockPrice result = parser.parse(message).orElseThrow();

    assertThat(result.changeAmountKrw()).isEqualTo(2500L);
    assertThat(result.changeRate()).isEqualByComparingTo(new BigDecimal("1.41"));
    assertThat(result.direction()).isEqualTo(StockPriceDirection.UP);
  }

  @Test
  void parseKisRealtimeStockPriceMessageWithFlatSign() {
    String message =
        "0|H0STCNT0|001|" + item("005930", "143000", "70000", "3", "100", "0.12", "20260513");

    StockPriceRealtimeParser.ParsedStockPrice result = parser.parse(message).orElseThrow();

    assertThat(result.changeAmountKrw()).isZero();
    assertThat(result.changeRate()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.direction()).isEqualTo(StockPriceDirection.FLAT);
  }

  @Test
  void parseMultipleKisRealtimeStockPriceMessages() {
    String message =
        "0|H0STCNT0|002|"
            + item("005930", "093354", "71900", "5", "-100", "-0.14", "20230612")
            + "^"
            + item("000660", "143000", "180000", "2", "2500", "1.41", "20230612");

    var result = parser.parseAll(message);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).ticker()).isEqualTo("005930");
    assertThat(result.get(1).ticker()).isEqualTo("000660");
  }

  private String item(
      String ticker,
      String tradeTime,
      String currentPrice,
      String signCode,
      String changeAmount,
      String changeRate,
      String businessDate) {
    String[] fields = new String[46];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = "0";
    }

    fields[0] = ticker;
    fields[1] = tradeTime;
    fields[2] = currentPrice;
    fields[3] = signCode;
    fields[4] = changeAmount;
    fields[5] = changeRate;
    fields[33] = businessDate;
    return String.join("^", fields);
  }
}
