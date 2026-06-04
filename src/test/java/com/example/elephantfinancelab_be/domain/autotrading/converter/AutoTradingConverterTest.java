package com.example.elephantfinancelab_be.domain.autotrading.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import org.junit.jupiter.api.Test;

class AutoTradingConverterTest {

  @Test
  void trimsStoredCsvValuesWhenReturningSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-1")
            .status(AutoTradingSessionStatus.RUNNING)
            .selectedTickers("005930, 000660 ")
            .recommendationIds("1, 2 ")
            .build();

    AutoTradingResDTO.Session result = AutoTradingConverter.toSession(session);

    assertThat(result.getSelectedTickers()).containsExactly("005930", "000660");
    assertThat(result.getRecommendationIds()).containsExactly(1L, 2L);
  }
}
