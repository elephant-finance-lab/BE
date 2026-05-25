package com.example.elephantfinancelab_be.domain.autotrading.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class AutoTradingEventTypeTest {

  @Test
  void parsesEventTypeWithLocaleRoot() {
    Locale previous = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
      assertThat(AutoTradingEventType.from("decision_completed"))
          .isEqualTo(AutoTradingEventType.DECISION_COMPLETED);
    } finally {
      Locale.setDefault(previous);
    }
  }
}
