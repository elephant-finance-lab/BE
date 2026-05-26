package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexPushService {

  private static final String DESTINATION = "/topic/market-indexes";

  private final SimpMessagingTemplate messagingTemplate;

  public void push(MarketIndexResDTO.MarketIndex index) {
    try {
      messagingTemplate.convertAndSend(DESTINATION, index);
      log.debug("시장 지수 실시간 push 완료. market={}", index.market());
    } catch (RuntimeException exception) {
      log.warn(
          "시장 지수 STOMP push 실패. market={}, destination={}", index.market(), DESTINATION, exception);
    }
  }
}
