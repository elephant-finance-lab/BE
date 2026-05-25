package com.example.elephantfinancelab_be.domain.notification.service.command;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationType;

public interface NotificationCommandService {

  NotificationResDTO.Item create(
      Long userId,
      NotificationType type,
      String title,
      String message,
      NotificationReferenceType referenceType,
      String referenceId);

  NotificationResDTO.Item markRead(Long userId, Long notificationId);

  NotificationResDTO.ReadAll markAllRead(Long userId);
}
