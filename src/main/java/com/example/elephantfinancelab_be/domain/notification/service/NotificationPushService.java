package com.example.elephantfinancelab_be.domain.notification.service;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

  private final SimpMessagingTemplate messagingTemplate;

  public void push(Long userId, NotificationResDTO.Item notification) {
    String destination = "/topic/users/" + userId + "/notifications";
    try {
      messagingTemplate.convertAndSend(destination, notification);
    } catch (RuntimeException exception) {
      log.warn("알림 STOMP push 실패. userId={}, destination={}", userId, destination, exception);
    }
  }
}
