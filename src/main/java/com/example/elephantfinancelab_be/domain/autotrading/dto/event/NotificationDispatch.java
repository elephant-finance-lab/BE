package com.example.elephantfinancelab_be.domain.autotrading.dto.event;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;

public record NotificationDispatch(Long userId, NotificationResDTO.Item notification) {}
