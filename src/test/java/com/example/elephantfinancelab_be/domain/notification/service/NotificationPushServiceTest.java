package com.example.elephantfinancelab_be.domain.notification.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class NotificationPushServiceTest {

  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final NotificationPushService service = new NotificationPushService(messagingTemplate);

  @Test
  void pushesNotificationThroughUserQueue() {
    NotificationResDTO.Item notification =
        NotificationResDTO.Item.builder().notificationId(1L).message("message").build();

    service.push(10L, notification);

    verify(messagingTemplate).convertAndSendToUser("10", "/queue/notifications", notification);
  }
}
