package com.example.elephantfinancelab_be.domain.notification.service.query;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import org.springframework.data.domain.Pageable;

public interface NotificationQueryService {

  NotificationResDTO.Page findNotifications(Long userId, Pageable pageable);
}
