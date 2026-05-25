package com.example.elephantfinancelab_be.domain.autotrading.repository;

import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoTradingSessionRepository extends JpaRepository<AutoTradingSession, Long> {

  Optional<AutoTradingSession> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

  Optional<AutoTradingSession> findBySessionIdAndUserId(String sessionId, Long userId);

  Optional<AutoTradingSession> findFirstByAiSessionIdOrderByCreatedAtDesc(String aiSessionId);

  Optional<AutoTradingSession> findFirstByAiRequestIdOrderByCreatedAtDesc(String aiRequestId);

  Optional<AutoTradingSession> findBySessionId(String sessionId);

  Optional<AutoTradingSession> findFirstByIdempotencyKeyOrderByCreatedAtDesc(String idempotencyKey);

  Optional<AutoTradingSession> findByActiveSlot(String activeSlot);

  boolean existsByActiveSlot(String activeSlot);
}
