package com.example.elephantfinancelab_be.domain.notification.service.command;

import com.example.elephantfinancelab_be.domain.notification.converter.NotificationConverter;
import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.entity.Notification;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationType;
import com.example.elephantfinancelab_be.domain.notification.exception.NotificationException;
import com.example.elephantfinancelab_be.domain.notification.exception.code.NotificationErrorCode;
import com.example.elephantfinancelab_be.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandServiceImpl implements NotificationCommandService {

  private final NotificationRepository notificationRepository;

  @Override
  public NotificationResDTO.Item create(
      Long userId,
      NotificationType type,
      String title,
      String message,
      NotificationReferenceType referenceType,
      String referenceId) {
    Notification notification =
        Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .message(message)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .build();
    return NotificationConverter.toItem(notificationRepository.save(notification));
  }

  @Override
  public NotificationResDTO.Item markRead(Long userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(
                () -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    notification.markRead();
    return NotificationConverter.toItem(notification);
  }

  @Override
  public NotificationResDTO.ReadAll markAllRead(Long userId) {
    List<Notification> unread = notificationRepository.findAllByUserIdAndReadFalse(userId);
    unread.forEach(Notification::markRead);
    return NotificationResDTO.ReadAll.builder().updatedCount(unread.size()).build();
  }
}
