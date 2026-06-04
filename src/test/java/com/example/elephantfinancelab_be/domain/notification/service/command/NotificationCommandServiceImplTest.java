package com.example.elephantfinancelab_be.domain.notification.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationCommandServiceImplTest {

  private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
  private final NotificationCommandServiceImpl service =
      new NotificationCommandServiceImpl(notificationRepository);

  @Test
  void marksAllReadWithBulkUpdate() {
    when(notificationRepository.markAllReadByUserId(org.mockito.ArgumentMatchers.eq(1L), any()))
        .thenReturn(3);

    NotificationResDTO.ReadAll result = service.markAllRead(1L);

    assertThat(result.getUpdatedCount()).isEqualTo(3);
    verify(notificationRepository)
        .markAllReadByUserId(org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class));
  }
}
