package com.example.elephantfinancelab_be.domain.notification.converter;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.domain.Page;

public final class NotificationConverter {

  private NotificationConverter() {}

  public static NotificationResDTO.Item toItem(Notification notification) {
    return NotificationResDTO.Item.builder()
        .notificationId(notification.getId())
        .type(notification.getType())
        .title(notification.getTitle())
        .message(notification.getMessage())
        .referenceType(notification.getReferenceType())
        .referenceId(notification.getReferenceId())
        .read(notification.isRead())
        .createdAt(notification.getCreatedAt())
        .readAt(notification.getReadAt())
        .build();
  }

  public static NotificationResDTO.Page toPage(Page<Notification> page) {
    List<NotificationResDTO.Item> notifications =
        page.getContent().stream().map(NotificationConverter::toItem).toList();
    return NotificationResDTO.Page.builder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .hasNext(page.hasNext())
        .notifications(notifications)
        .build();
  }
}
