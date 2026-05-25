package com.example.elephantfinancelab_be.domain.autotrading.listener;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.NotificationDispatch;
import com.example.elephantfinancelab_be.domain.autotrading.service.event.AutoTradingEventProcessingService;
import com.example.elephantfinancelab_be.domain.notification.service.NotificationPushService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "auto-trading.kafka.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AutoTradingKafkaConsumer {

  private final AutoTradingEventProcessingService eventProcessingService;
  private final NotificationPushService notificationPushService;

  @KafkaListener(topics = "${auto-trading.kafka.topic}", groupId = "${auto-trading.kafka.group-id}")
  public void consume(ConsumerRecord<String, String> record) {
    try {
      Optional<NotificationDispatch> dispatch =
          eventProcessingService.process(record.key(), record.value());
      dispatch.ifPresent(item -> notificationPushService.push(item.userId(), item.notification()));
    } catch (DataIntegrityViolationException duplicate) {
      log.debug("동시에 재수신된 자동매매 Kafka 이벤트를 unique constraint로 건너뜁니다. key={}", record.key());
    }
  }
}
