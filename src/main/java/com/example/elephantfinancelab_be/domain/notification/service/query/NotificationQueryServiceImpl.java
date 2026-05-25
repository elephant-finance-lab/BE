package com.example.elephantfinancelab_be.domain.notification.service.query;

import com.example.elephantfinancelab_be.domain.notification.converter.NotificationConverter;
import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

  private final NotificationRepository notificationRepository;

  @Override
  public NotificationResDTO.Page findNotifications(Long userId, Pageable pageable) {
    return NotificationConverter.toPage(
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable));
  }
}
