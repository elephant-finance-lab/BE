package com.example.elephantfinancelab_be.domain.autotrading.repository;

import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoTradingEventRepository extends JpaRepository<AutoTradingEvent, Long> {

  boolean existsByEventId(String eventId);

  boolean existsBySyntheticEventId(String syntheticEventId);
}
