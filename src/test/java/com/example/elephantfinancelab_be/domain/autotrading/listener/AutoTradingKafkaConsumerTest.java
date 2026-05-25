package com.example.elephantfinancelab_be.domain.autotrading.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.autotrading.service.event.AutoTradingEventProcessingService;
import com.example.elephantfinancelab_be.domain.notification.service.NotificationPushService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AutoTradingKafkaConsumerTest {

  private final AutoTradingEventProcessingService eventProcessingService =
      mock(AutoTradingEventProcessingService.class);
  private final NotificationPushService notificationPushService =
      mock(NotificationPushService.class);
  private final AutoTradingKafkaConsumer consumer =
      new AutoTradingKafkaConsumer(eventProcessingService, notificationPushService);

  @Test
  void ignoresKnownDuplicateEventConstraint() {
    when(eventProcessingService.process(anyString(), anyString()))
        .thenThrow(
            new DataIntegrityViolationException("uk_auto_trading_event_synthetic_id duplicate"));

    consumer.consume(record());

    verifyNoInteractions(notificationPushService);
  }

  @Test
  void rethrowsUnexpectedIntegrityViolation() {
    when(eventProcessingService.process(anyString(), anyString()))
        .thenThrow(new DataIntegrityViolationException("not-null violation"));

    assertThatThrownBy(() -> consumer.consume(record()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static ConsumerRecord<String, String> record() {
    return new ConsumerRecord<>("elephant-paper-trading-events", 0, 0L, "key", "{}");
  }
}
