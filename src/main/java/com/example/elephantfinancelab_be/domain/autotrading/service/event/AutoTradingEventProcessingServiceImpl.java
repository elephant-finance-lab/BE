package com.example.elephantfinancelab_be.domain.autotrading.service.event;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.AutoTradingKafkaEvent;
import com.example.elephantfinancelab_be.domain.autotrading.dto.event.NotificationDispatch;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingExecution;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingExecutionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingEventRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingExecutionRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationType;
import com.example.elephantfinancelab_be.domain.notification.service.command.NotificationCommandService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoTradingEventProcessingServiceImpl implements AutoTradingEventProcessingService {

  private static final String ACTIVE_SLOT = "SHARED_KIS_VIRTUAL_ACCOUNT";

  private final AutoTradingKafkaEventParser eventParser;
  private final AutoTradingEventRepository eventRepository;
  private final AutoTradingExecutionRepository executionRepository;
  private final AutoTradingSessionRepository sessionRepository;
  private final NotificationCommandService notificationCommandService;

  @Override
  @Transactional
  public Optional<NotificationDispatch> process(String messageKey, String rawEventJson) {
    AutoTradingKafkaEvent message = eventParser.parse(messageKey, rawEventJson);
    if (alreadyProcessed(message)) {
      log.debug("중복 자동매매 이벤트를 건너뜁니다. eventReference={}", message.syntheticEventId());
      return Optional.empty();
    }

    Optional<AutoTradingSession> matchedSession = findSession(message);
    AutoTradingEvent event =
        eventRepository.saveAndFlush(
            toEvent(message, matchedSession.map(AutoTradingSession::getSessionId).orElse(null)));
    AutoTradingExecution execution =
        saveExecutionIfRequired(message, event, matchedSession.orElse(null));

    if (message.eventType() == AutoTradingEventType.UNKNOWN) {
      log.warn("알 수 없는 자동매매 Kafka eventType을 저장했습니다. rawEventType={}", message.rawEventType());
      return Optional.empty();
    }
    if (message.eventType() == AutoTradingEventType.SCHEDULER_AUDIT) {
      log.debug("서버 자동매매 운영 audit 이벤트는 사용자 알림으로 변환하지 않습니다. messageKey={}", message.messageKey());
      return Optional.empty();
    }
    if (matchedSession.isEmpty()) {
      log.warn(
          "세션과 매칭되지 않은 자동매매 Kafka 이벤트를 저장했습니다. eventType={}, aiSessionId={}, messageKey={}",
          message.eventType(),
          message.aiSessionId(),
          message.messageKey());
      return Optional.empty();
    }

    AutoTradingSession session = matchedSession.get();
    boolean stoppedAfterRequest = stoppedAfterRequest(session, message);
    boolean failedTerminalStop =
        message.eventType() == AutoTradingEventType.AUTO_TRADING_STOPPED
            && isFailedStatus(payloadText(message.payload(), "terminal_status", "terminalStatus"));
    applySessionStatus(session, message);
    NotificationResDTO.Item notification =
        createNotification(
            session, event, execution, message, stoppedAfterRequest, failedTerminalStop);
    return Optional.of(new NotificationDispatch(session.getUserId(), notification));
  }

  private boolean alreadyProcessed(AutoTradingKafkaEvent message) {
    return (message.eventId() != null && eventRepository.existsByEventId(message.eventId()))
        || eventRepository.existsBySyntheticEventId(message.syntheticEventId());
  }

  private Optional<AutoTradingSession> findSession(AutoTradingKafkaEvent message) {
    Optional<AutoTradingSession> session =
        findByAiSessionId(message.aiSessionId())
            .or(() -> findByAiRequestId(message.requestId()))
            .or(() -> findByBeSessionId(message.beSessionId()))
            .or(() -> findByBeSessionId(message.correlationId()))
            .or(() -> findByAiSessionId(message.messageKey()))
            .or(() -> findByBeSessionId(message.messageKey()));
    if (session.isPresent()) {
      return session;
    }

    // AI START event can arrive before the synchronous gRPC response exposes aiSessionId to BE.
    // The paper demo permits only one STARTING shared-account session, so it is a safe fallback
    // here.
    if (message.eventType() == AutoTradingEventType.AUTO_TRADING_STARTED) {
      return sessionRepository
          .findByActiveSlot(ACTIVE_SLOT)
          .filter(item -> item.getStatus() == AutoTradingSessionStatus.STARTING);
    }
    return Optional.empty();
  }

  private boolean stoppedAfterRequest(AutoTradingSession session, AutoTradingKafkaEvent message) {
    if (message.eventType() == AutoTradingEventType.AUTO_TRADING_FAILED) {
      return session.getStatus() == AutoTradingSessionStatus.STOPPING;
    }
    if (message.eventType() != AutoTradingEventType.AUTO_TRADING_STOPPED) {
      return false;
    }

    String stopReason = payloadText(message.payload(), "stop_reason", "stopReason");
    return "USER_REQUESTED".equalsIgnoreCase(stopReason)
        || (stopReason == null && session.getStatus() == AutoTradingSessionStatus.STOPPING);
  }

  private Optional<AutoTradingSession> findByAiSessionId(String value) {
    return value == null
        ? Optional.empty()
        : sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc(value);
  }

  private Optional<AutoTradingSession> findByBeSessionId(String value) {
    return value == null ? Optional.empty() : sessionRepository.findBySessionId(value);
  }

  private Optional<AutoTradingSession> findByAiRequestId(String value) {
    return value == null
        ? Optional.empty()
        : sessionRepository.findFirstByAiRequestIdOrderByCreatedAtDesc(value);
  }

  private AutoTradingEvent toEvent(AutoTradingKafkaEvent message, String beSessionId) {
    return AutoTradingEvent.builder()
        .eventId(message.eventId())
        .syntheticEventId(message.syntheticEventId())
        .eventType(message.eventType())
        .beSessionId(beSessionId)
        .aiSessionId(message.aiSessionId())
        .requestId(message.requestId())
        .idempotencyKey(message.idempotencyKey())
        .bundleId(message.bundleId())
        .messageKey(message.messageKey())
        .payloadJson(message.payloadJson())
        .rawEventJson(message.rawEventJson())
        .occurredAt(message.occurredAt())
        .receivedAt(LocalDateTime.now())
        .build();
  }

  private AutoTradingExecution saveExecutionIfRequired(
      AutoTradingKafkaEvent message, AutoTradingEvent event, AutoTradingSession session) {
    if (!message.eventType().isOrderEvent()) {
      return null;
    }
    AutoTradingExecution execution =
        AutoTradingExecution.builder()
            .sessionId(session == null ? null : session.getSessionId())
            .eventReference(event.getSyntheticEventId())
            .eventType(message.eventType())
            .ticker(payloadText(message.payload(), "ticker", "ticker_code", "tickerCode"))
            .side(payloadText(message.payload(), "side"))
            .quantity(payloadLong(message.payload(), "quantity", "qty", "order_qty", "orderQty"))
            .price(payloadDecimal(message.payload(), "price", "order_price", "orderPrice"))
            .orderType(payloadText(message.payload(), "order_type", "orderType"))
            .orderPlanId(payloadText(message.payload(), "order_plan_id", "orderPlanId"))
            .kisOrderNo(payloadText(message.payload(), "kis_order_no", "kisOrderNo"))
            .brokerOrderId(
                payloadText(message.payload(), "broker_order_id", "brokerOrderId", "order_id"))
            .executionMode(payloadText(message.payload(), "execution_mode", "executionMode"))
            .kisMode(payloadText(message.payload(), "kis_mode", "kisMode"))
            .paperOnly(payloadBoolean(message.payload(), "paper_only", "paperOnly"))
            .orderStatus(executionStatus(message.eventType()))
            .filledQuantity(payloadLong(message.payload(), "filled_quantity", "filledQuantity"))
            .filledPrice(
                payloadDecimal(message.payload(), "filled_price", "filledPrice", "avg_fill_price"))
            .message(payloadText(message.payload(), "message", "reason", "error"))
            .errorCode(payloadText(message.payload(), "error_code", "errorCode"))
            .occurredAt(message.occurredAt())
            .build();
    return executionRepository.saveAndFlush(execution);
  }

  private void applySessionStatus(AutoTradingSession session, AutoTradingKafkaEvent message) {
    String statusMessage = statusMessage(message);
    switch (message.eventType()) {
      case AUTO_TRADING_STARTED -> {
        if (!session.isTerminal() && session.getStatus() != AutoTradingSessionStatus.STOPPING) {
          String aiSessionId =
              firstNonBlank(message.aiSessionId(), message.messageKey(), session.getAiSessionId());
          LocalDateTime startedAt =
              message.occurredAt() == null ? LocalDateTime.now() : message.occurredAt();
          session.markRunning(aiSessionId, statusMessage, startedAt);
          sessionRepository.saveAndFlush(session);
        }
      }
      case PAPER_ORDER_FAILED -> {
        session.updateAiStatusMessage(statusMessage);
        sessionRepository.saveAndFlush(session);
      }
      case AUTO_TRADING_STOPPED -> {
        String terminalStatus = payloadText(message.payload(), "terminal_status", "terminalStatus");
        String stopReason = payloadText(message.payload(), "stop_reason", "stopReason");
        if (isFailedStatus(terminalStatus)) {
          session.markFailed(statusMessage);
        } else if ("COMPLETED".equalsIgnoreCase(stopReason)) {
          session.markCompleted(statusMessage);
        } else {
          session.markStopped(statusMessage);
        }
        sessionRepository.saveAndFlush(session);
      }
      case AUTO_TRADING_FAILED -> {
        if (session.getStatus() == AutoTradingSessionStatus.STOPPING) {
          // Backward compatibility for events from AI versions that represented Stop as failure.
          session.markStopped("Stop 요청 이후 종료됨");
        } else {
          session.markFailed(statusMessage);
        }
        sessionRepository.saveAndFlush(session);
      }
      default -> {
        // Decision and successfully submitted/filled order events do not alter lifecycle state.
      }
    }
  }

  private NotificationResDTO.Item createNotification(
      AutoTradingSession session,
      AutoTradingEvent event,
      AutoTradingExecution execution,
      AutoTradingKafkaEvent message,
      boolean stoppedAfterRequest,
      boolean failedTerminalStop) {
    NotificationContent content =
        notificationContent(message, stoppedAfterRequest, failedTerminalStop);
    NotificationReferenceType referenceType =
        execution == null
            ? referenceType(message.eventType())
            : NotificationReferenceType.PAPER_ORDER_EXECUTION;
    String referenceId =
        execution == null
            ? referenceId(session, event, message.eventType())
            : String.valueOf(execution.getId());
    return notificationCommandService.create(
        session.getUserId(),
        content.type(),
        content.title(),
        content.message(),
        referenceType,
        referenceId);
  }

  private NotificationContent notificationContent(
      AutoTradingKafkaEvent event, boolean stoppedAfterRequest, boolean failedTerminalStop) {
    if (stoppedAfterRequest) {
      return new NotificationContent(
          NotificationType.AUTO_TRADING, "자동매매 종료", "AI 모의 자동매매가 중지 요청 이후 종료되었습니다.");
    }
    if (failedTerminalStop) {
      return new NotificationContent(
          NotificationType.AUTO_TRADING, "자동매매 오류", "AI 모의 자동매매가 오류로 중단되었습니다.");
    }
    String orderDescription = orderDescription(event.payload());
    return switch (event.eventType()) {
      case AUTO_TRADING_STARTED ->
          new NotificationContent(NotificationType.AUTO_TRADING, "자동매매 시작", "AI 모의 자동매매가 시작되었습니다.");
      case DECISION_COMPLETED ->
          new NotificationContent(
              NotificationType.AUTO_TRADING, "AI 판단 완료", decisionMessage(event.payload()));
      case PAPER_ORDER_SUBMITTED ->
          new NotificationContent(
              NotificationType.PAPER_ORDER, "모의 주문 접수", orderMessage(orderDescription, "접수되었습니다."));
      case PAPER_ORDER_FILLED ->
          new NotificationContent(
              NotificationType.PAPER_ORDER, "모의 주문 체결", orderMessage(orderDescription, "체결되었습니다."));
      case PAPER_ORDER_FAILED ->
          new NotificationContent(
              NotificationType.PAPER_ORDER,
              "모의 주문 실패",
              orderMessage(orderDescription, "처리에 실패했습니다."));
      case AUTO_TRADING_STOPPED ->
          new NotificationContent(NotificationType.AUTO_TRADING, "자동매매 종료", "AI 모의 자동매매가 종료되었습니다.");
      case AUTO_TRADING_FAILED ->
          new NotificationContent(
              NotificationType.AUTO_TRADING, "자동매매 오류", "AI 모의 자동매매가 오류로 중단되었습니다.");
      case SCHEDULER_AUDIT ->
          new NotificationContent(NotificationType.SYSTEM, "자동매매 운영 로그", "서버 자동매매 운영 상태가 기록되었습니다.");
      case UNKNOWN ->
          new NotificationContent(NotificationType.SYSTEM, "알 수 없는 이벤트", "처리할 수 없는 자동매매 이벤트입니다.");
    };
  }

  private String statusMessage(AutoTradingKafkaEvent event) {
    String detail =
        payloadText(event.payload(), "stop_reason", "stopReason", "reason", "error", "message");
    return detail == null ? event.eventType().name() : event.eventType().name() + ": " + detail;
  }

  private static NotificationReferenceType referenceType(AutoTradingEventType eventType) {
    return eventType == AutoTradingEventType.DECISION_COMPLETED
        ? NotificationReferenceType.AUTO_TRADING_EVENT
        : NotificationReferenceType.AUTO_TRADING_SESSION;
  }

  private static String referenceId(
      AutoTradingSession session, AutoTradingEvent event, AutoTradingEventType eventType) {
    return eventType == AutoTradingEventType.DECISION_COMPLETED
        ? event.getSyntheticEventId()
        : session.getSessionId();
  }

  private static AutoTradingExecutionStatus executionStatus(AutoTradingEventType eventType) {
    return switch (eventType) {
      case PAPER_ORDER_SUBMITTED -> AutoTradingExecutionStatus.SUBMITTED;
      case PAPER_ORDER_FILLED -> AutoTradingExecutionStatus.FILLED;
      case PAPER_ORDER_FAILED -> AutoTradingExecutionStatus.FAILED;
      default -> throw new IllegalArgumentException("주문 이벤트가 아닙니다: " + eventType);
    };
  }

  private static String decisionMessage(JsonNode payload) {
    JsonNode approved = value(payload, "approved");
    Long orderCount = payloadLong(payload, "order_count", "orderCount");
    if ((approved != null && approved.isBoolean() && !approved.asBoolean())
        || (orderCount != null && orderCount == 0)) {
      return "AI가 판단을 완료했습니다. 현재 조건에서는 주문을 실행하지 않았습니다.";
    }
    return "AI가 매매 판단을 완료했습니다.";
  }

  private static String orderDescription(JsonNode payload) {
    String ticker = payloadText(payload, "ticker", "ticker_code", "tickerCode");
    String side = payloadText(payload, "side");
    String quantity =
        payloadText(
            payload,
            "quantity",
            "qty",
            "order_qty",
            "orderQty",
            "filled_quantity",
            "filledQuantity");
    if (ticker == null && side == null && quantity == null) {
      return null;
    }
    StringBuilder description = new StringBuilder();
    if (ticker != null) {
      description.append(ticker).append(" ");
    }
    if (quantity != null) {
      description.append(quantity).append("주 ");
    }
    if (side != null) {
      description.append("모의 ").append(koreanSide(side)).append(" ");
    } else {
      description.append("모의 ");
    }
    description.append("주문이");
    return description.toString();
  }

  private static String orderMessage(String description, String suffix) {
    return description == null ? "모의 주문이 " + suffix : description + " " + suffix;
  }

  private static String koreanSide(String side) {
    return switch (side.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "BUY" -> "매수";
      case "SELL" -> "매도";
      default -> side.trim();
    };
  }

  private static String payloadText(JsonNode payload, String... fields) {
    JsonNode value = value(payload, fields);
    return value == null || value.asText().isBlank() ? null : value.asText().trim();
  }

  private static Long payloadLong(JsonNode payload, String... fields) {
    String value = payloadText(payload, fields);
    if (value == null) {
      return null;
    }
    try {
      return Long.valueOf(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static BigDecimal payloadDecimal(JsonNode payload, String... fields) {
    String value = payloadText(payload, fields);
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private static Boolean payloadBoolean(JsonNode payload, String... fields) {
    JsonNode value = value(payload, fields);
    return value == null ? null : value.asBoolean();
  }

  private static boolean isFailedStatus(String status) {
    return "FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
  }

  private static JsonNode value(JsonNode payload, String... fields) {
    if (payload == null || !payload.isObject()) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = payload.get(field);
      if (value != null && !value.isNull()) {
        return value;
      }
    }
    return null;
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private record NotificationContent(NotificationType type, String title, String message) {}
}
