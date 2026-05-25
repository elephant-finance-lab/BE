package com.example.elephantfinancelab_be.domain.autotrading.service.event;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.NotificationDispatch;
import java.util.Optional;

public interface AutoTradingEventProcessingService {

  Optional<NotificationDispatch> process(String messageKey, String rawEventJson);
}
