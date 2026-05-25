package com.example.elephantfinancelab_be.domain.notification.dto.res;

import com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class NotificationResDTO {

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Item {
    private Long notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private NotificationReferenceType referenceType;
    private String referenceId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class Page {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private List<Item> notifications;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class ReadAll {
    private int updatedCount;
  }
}
