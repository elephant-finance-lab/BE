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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "auto_trading_executions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_auto_trading_execution_event",
            columnNames = "event_reference"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AutoTradingExecution extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", length = 36)
  private String sessionId;

  @Column(name = "event_reference", nullable = false, length = 160)
  private String eventReference;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private AutoTradingEventType eventType;

  @Column(name = "ticker", length = 20)
  private String ticker;

  @Column(name = "side", length = 20)
  private String side;

  @Column(name = "quantity")
  private Long quantity;

  @Column(name = "price", precision = 19, scale = 4)
  private BigDecimal price;

  @Column(name = "order_type", length = 30)
  private String orderType;

  @Column(name = "order_plan_id", length = 100)
  private String orderPlanId;

  @Column(name = "kis_order_no", length = 100)
  private String kisOrderNo;

  @Column(name = "broker_order_id", length = 100)
  private String brokerOrderId;

  @Column(name = "execution_mode", length = 20)
  private String executionMode;

  @Column(name = "kis_mode", length = 20)
  private String kisMode;

  @Column(name = "paper_only")
  private Boolean paperOnly;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status", nullable = false, length = 20)
  private AutoTradingExecutionStatus orderStatus;

  @Column(name = "filled_quantity")
  private Long filledQuantity;

  @Column(name = "filled_price", precision = 19, scale = 4)
  private BigDecimal filledPrice;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "error_code", length = 100)
  private String errorCode;

  @Column(name = "occurred_at")
  private LocalDateTime occurredAt;
}
