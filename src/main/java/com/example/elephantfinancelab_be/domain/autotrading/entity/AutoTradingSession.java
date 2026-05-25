package com.example.elephantfinancelab_be.domain.autotrading.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "auto_trading_sessions",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_auto_trading_session_id", columnNames = "session_id"),
      @UniqueConstraint(
          name = "uk_auto_trading_user_idempotency",
          columnNames = {"user_id", "idempotency_key"}),
      @UniqueConstraint(name = "uk_auto_trading_active_slot", columnNames = "active_slot")
    })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoTradingSession extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false, length = 36)
  private String sessionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AutoTradingSessionStatus status;

  @Column(name = "selected_tickers", nullable = false, columnDefinition = "TEXT")
  private String selectedTickers;

  @Column(name = "recommendation_ids", nullable = false, columnDefinition = "TEXT")
  private String recommendationIds;

  @Column(name = "purchase_option_id", nullable = false)
  private Integer purchaseOptionId;

  @Column(name = "idempotency_key", nullable = false, length = 120)
  private String idempotencyKey;

  @Column(name = "ai_request_id", length = 36)
  private String aiRequestId;

  @Column(name = "ai_session_id", length = 100)
  private String aiSessionId;

  @Column(name = "ai_status_message", columnDefinition = "TEXT")
  private String aiStatusMessage;

  @Column(name = "active_slot", length = 50)
  private String activeSlot;

  @Column(name = "started_at")
  private LocalDateTime startedAt;

  @Column(name = "stopped_at")
  private LocalDateTime stoppedAt;

  @Column(name = "failed_at")
  private LocalDateTime failedAt;

  public void markRunning(String aiSessionId, String message, LocalDateTime startedAt) {
    this.status = AutoTradingSessionStatus.RUNNING;
    this.aiSessionId = aiSessionId;
    this.aiStatusMessage = message;
    this.startedAt = startedAt;
  }

  public void markStartFailed(String message) {
    this.status = AutoTradingSessionStatus.FAILED;
    this.aiStatusMessage = message;
    this.failedAt = LocalDateTime.now();
    releaseActiveSlot();
  }

  public void markStopping(String message) {
    this.status = AutoTradingSessionStatus.STOPPING;
    this.aiStatusMessage = message;
  }

  public void restoreStatus(AutoTradingSessionStatus previousStatus, String message) {
    this.status = previousStatus;
    this.aiStatusMessage = message;
  }

  public void markStopped(String message) {
    this.status = AutoTradingSessionStatus.STOPPED;
    this.aiStatusMessage = message;
    this.stoppedAt = LocalDateTime.now();
    releaseActiveSlot();
  }

  public void markCompleted(String message) {
    this.status = AutoTradingSessionStatus.COMPLETED;
    this.aiStatusMessage = message;
    this.stoppedAt = LocalDateTime.now();
    releaseActiveSlot();
  }

  public void updateAiStatusMessage(String message) {
    this.aiStatusMessage = message;
  }

  public boolean isTerminal() {
    return status == AutoTradingSessionStatus.STOPPED
        || status == AutoTradingSessionStatus.FAILED
        || status == AutoTradingSessionStatus.COMPLETED;
  }

  private void releaseActiveSlot() {
    this.activeSlot = null;
  }
}
