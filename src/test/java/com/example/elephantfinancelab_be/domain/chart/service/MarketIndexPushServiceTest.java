package com.example.elephantfinancelab_be.domain.chart.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class MarketIndexPushServiceTest {

  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final MarketIndexPushService service = new MarketIndexPushService(messagingTemplate);

  @Test
  void pushesMarketIndexThroughPublicTopic() {
    MarketIndexResDTO.MarketIndex index =
        new MarketIndexResDTO.MarketIndex(
            "KOSPI",
            new BigDecimal("2735.24"),
            new BigDecimal("12.31"),
            new BigDecimal("0.45"),
            LocalDateTime.of(2026, 5, 26, 13, 45));

    service.push(index);

    verify(messagingTemplate).convertAndSend("/topic/market-indexes", index);
  }
}
