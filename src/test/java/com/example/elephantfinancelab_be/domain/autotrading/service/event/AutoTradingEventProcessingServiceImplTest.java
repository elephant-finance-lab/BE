package com.example.elephantfinancelab_be.domain.autotrading.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.autotrading.dto.event.AutoTradingKafkaEvent;
import com.example.elephantfinancelab_be.domain.autotrading.dto.event.NotificationDispatch;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingExecution;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingEventRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingExecutionRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.entity.NotificationType;
import com.example.elephantfinancelab_be.domain.notification.service.command.NotificationCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AutoTradingEventProcessingServiceImplTest {

  private final AutoTradingKafkaEventParser parser = mock(AutoTradingKafkaEventParser.class);
  private final AutoTradingEventRepository eventRepository = mock(AutoTradingEventRepository.class);
  private final AutoTradingExecutionRepository executionRepository =
      mock(AutoTradingExecutionRepository.class);
  private final AutoTradingSessionRepository sessionRepository =
      mock(AutoTradingSessionRepository.class);
  private final NotificationCommandService notificationCommandService =
      mock(NotificationCommandService.class);
  private final AutoTradingEventProcessingServiceImpl service =
      new AutoTradingEventProcessingServiceImpl(
          parser,
          eventRepository,
          executionRepository,
          sessionRepository,
          notificationCommandService);

  @BeforeEach
  void setUp() {
    when(eventRepository.saveAndFlush(any(AutoTradingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(executionRepository.saveAndFlush(any(AutoTradingExecution.class)))
        .thenAnswer(
            invocation -> {
              AutoTradingExecution execution = invocation.getArgument(0);
              ReflectionTestUtils.setField(execution, "id", 99L);
              return execution;
            });
    when(notificationCommandService.create(any(), any(), any(), any(), any(), any()))
        .thenReturn(NotificationResDTO.Item.builder().notificationId(1L).build());
  }

  @Test
  void treatsFailureAfterStopRequestAsStoppedAndDispatchesNotification() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-1")
            .userId(1L)
            .aiSessionId("ai-session-1")
            .status(AutoTradingSessionStatus.STOPPING)
            .build();
    AutoTradingKafkaEvent event = event(AutoTradingEventType.AUTO_TRADING_FAILED, "ai-session-1");
    when(parser.parse("ai-session-1", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-1"))
        .thenReturn(Optional.of(session));

    Optional<NotificationDispatch> result = service.process("ai-session-1", "{}");

    assertThat(result).isPresent();
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.STOPPED);
    assertThat(session.getAiStatusMessage()).isEqualTo("Stop 요청 이후 종료됨");
    verify(sessionRepository).saveAndFlush(session);
    verify(notificationCommandService)
        .create(
            1L,
            NotificationType.AUTO_TRADING,
            "자동매매 종료",
            "AI 모의 자동매매가 중지 요청 이후 종료되었습니다.",
            com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType
                .AUTO_TRADING_SESSION,
            "be-session-1");
  }

  @Test
  void savesUnmatchedEventWithoutCreatingNotification() {
    AutoTradingKafkaEvent event = event(AutoTradingEventType.DECISION_COMPLETED, "unknown-session");
    when(parser.parse("unknown-session", "{}")).thenReturn(event);

    Optional<NotificationDispatch> result = service.process("unknown-session", "{}");

    assertThat(result).isEmpty();
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
    verify(notificationCommandService, never()).create(any(), any(), any(), any(), any(), any());
  }

  @Test
  void schedulerAuditEventIsPersistedWithoutUserNotification() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-audit")
            .userId(1L)
            .aiSessionId("ai-session-audit")
            .status(AutoTradingSessionStatus.RUNNING)
            .build();
    AutoTradingKafkaEvent event = event(AutoTradingEventType.SCHEDULER_AUDIT, "ai-session-audit");
    when(parser.parse("ai-session-audit", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-audit"))
        .thenReturn(Optional.of(session));

    Optional<NotificationDispatch> result = service.process("ai-session-audit", "{}");

    assertThat(result).isEmpty();
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
    verify(notificationCommandService, never()).create(any(), any(), any(), any(), any(), any());
  }

  @Test
  void matchesStartedEventUsingAiRequestIdAndStoresCorrelation() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-2")
            .userId(2L)
            .aiRequestId("start-request-2")
            .status(AutoTradingSessionStatus.STARTING)
            .build();
    AutoTradingKafkaEvent event =
        event(AutoTradingEventType.AUTO_TRADING_STARTED, "ai-session-2", "start-request-2");
    when(parser.parse("ai-session-2", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-2"))
        .thenReturn(Optional.empty());
    when(sessionRepository.findFirstByAiRequestIdOrderByCreatedAtDesc("start-request-2"))
        .thenReturn(Optional.of(session));

    Optional<NotificationDispatch> result = service.process("ai-session-2", "{}");

    assertThat(result).isPresent();
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.RUNNING);
    assertThat(session.getAiSessionId()).isEqualTo("ai-session-2");
    ArgumentCaptor<AutoTradingEvent> savedEvent = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(savedEvent.capture());
    assertThat(savedEvent.getValue().getRequestId()).isEqualTo("start-request-2");
  }

  @Test
  void savesEnhancedPaperFillProjectionAndUsesFilledQuantityInNotification() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-3")
            .userId(3L)
            .aiSessionId("ai-session-3")
            .status(AutoTradingSessionStatus.RUNNING)
            .build();
    var payload =
        new ObjectMapper()
            .createObjectNode()
            .put("ticker", "005930")
            .put("side", "BUY")
            .put("filled_quantity", 1)
            .put("filled_price", 72500)
            .put("order_plan_id", "plan-1")
            .put("kis_order_no", "KIS-1")
            .put("execution_mode", "paper")
            .put("kis_mode", "virtual")
            .put("paper_only", true);
    AutoTradingKafkaEvent event =
        new AutoTradingKafkaEvent(
            "event-3",
            "sha256:event-3",
            AutoTradingEventType.PAPER_ORDER_FILLED,
            AutoTradingEventType.PAPER_ORDER_FILLED.name(),
            "ai-session-3",
            "start-request-3",
            null,
            null,
            null,
            "BUNDLE-1",
            "ai-session-3",
            "{}",
            "{}",
            payload,
            LocalDateTime.of(2026, 5, 25, 12, 0));
    when(parser.parse("ai-session-3", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-3"))
        .thenReturn(Optional.of(session));

    service.process("ai-session-3", "{}");

    ArgumentCaptor<AutoTradingExecution> execution =
        ArgumentCaptor.forClass(AutoTradingExecution.class);
    verify(executionRepository).saveAndFlush(execution.capture());
    assertThat(execution.getValue().getFilledQuantity()).isEqualTo(1L);
    assertThat(execution.getValue().getOrderPlanId()).isEqualTo("plan-1");
    assertThat(execution.getValue().getPaperOnly()).isTrue();
    verify(notificationCommandService)
        .create(
            3L,
            NotificationType.PAPER_ORDER,
            "모의 주문 체결",
            "005930 1주 모의 매수 주문이 체결되었습니다.",
            com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType
                .PAPER_ORDER_EXECUTION,
            "99");
  }

  @Test
  void describesDedicatedUserStopEventAsRequestedStop() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-4")
            .userId(4L)
            .aiSessionId("ai-session-4")
            .status(AutoTradingSessionStatus.STOPPING)
            .build();
    var payload = new ObjectMapper().createObjectNode().put("stop_reason", "USER_REQUESTED");
    AutoTradingKafkaEvent event =
        new AutoTradingKafkaEvent(
            "event-4",
            "sha256:event-4",
            AutoTradingEventType.AUTO_TRADING_STOPPED,
            AutoTradingEventType.AUTO_TRADING_STOPPED.name(),
            "ai-session-4",
            "start-request-4",
            null,
            null,
            null,
            "BUNDLE-1",
            "ai-session-4",
            "{}",
            "{}",
            payload,
            LocalDateTime.of(2026, 5, 25, 12, 0));
    when(parser.parse("ai-session-4", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-4"))
        .thenReturn(Optional.of(session));

    service.process("ai-session-4", "{}");

    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.STOPPED);
    verify(notificationCommandService)
        .create(
            4L,
            NotificationType.AUTO_TRADING,
            "자동매매 종료",
            "AI 모의 자동매매가 중지 요청 이후 종료되었습니다.",
            com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType
                .AUTO_TRADING_SESSION,
            "be-session-4");
  }

  @Test
  void mapsCompletedTerminalStopEventToCompletedSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-completed")
            .userId(5L)
            .aiSessionId("ai-session-completed")
            .status(AutoTradingSessionStatus.RUNNING)
            .build();
    var payload =
        new ObjectMapper()
            .createObjectNode()
            .put("stop_reason", "COMPLETED")
            .put("terminal_status", "PASS");
    AutoTradingKafkaEvent event =
        new AutoTradingKafkaEvent(
            "event-completed",
            "sha256:event-completed",
            AutoTradingEventType.AUTO_TRADING_STOPPED,
            AutoTradingEventType.AUTO_TRADING_STOPPED.name(),
            "ai-session-completed",
            "start-request-completed",
            null,
            null,
            null,
            "BUNDLE-1",
            "ai-session-completed",
            "{}",
            "{}",
            payload,
            LocalDateTime.of(2026, 5, 25, 12, 0));
    when(parser.parse("ai-session-completed", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-completed"))
        .thenReturn(Optional.of(session));

    service.process("ai-session-completed", "{}");

    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.COMPLETED);
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void mapsFailedTerminalStopEventToFailedSessionAndErrorNotification() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-failed")
            .userId(6L)
            .aiSessionId("ai-session-failed")
            .status(AutoTradingSessionStatus.RUNNING)
            .build();
    var payload =
        new ObjectMapper()
            .createObjectNode()
            .put("stop_reason", "COMPLETED")
            .put("terminal_status", "FAIL");
    AutoTradingKafkaEvent event =
        new AutoTradingKafkaEvent(
            "event-failed",
            "sha256:event-failed",
            AutoTradingEventType.AUTO_TRADING_STOPPED,
            AutoTradingEventType.AUTO_TRADING_STOPPED.name(),
            "ai-session-failed",
            "start-request-failed",
            null,
            null,
            null,
            "BUNDLE-1",
            "ai-session-failed",
            "{}",
            "{}",
            payload,
            LocalDateTime.of(2026, 5, 25, 12, 0));
    when(parser.parse("ai-session-failed", "{}")).thenReturn(event);
    when(sessionRepository.findFirstByAiSessionIdOrderByCreatedAtDesc("ai-session-failed"))
        .thenReturn(Optional.of(session));

    service.process("ai-session-failed", "{}");

    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    verify(notificationCommandService)
        .create(
            6L,
            NotificationType.AUTO_TRADING,
            "자동매매 오류",
            "AI 모의 자동매매가 오류로 중단되었습니다.",
            com.example.elephantfinancelab_be.domain.notification.entity.NotificationReferenceType
                .AUTO_TRADING_SESSION,
            "be-session-failed");
  }

  private static AutoTradingKafkaEvent event(AutoTradingEventType type, String aiSessionId) {
    return event(type, aiSessionId, null);
  }

  private static AutoTradingKafkaEvent event(
      AutoTradingEventType type, String aiSessionId, String requestId) {
    return new AutoTradingKafkaEvent(
        null,
        "sha256:event-1",
        type,
        type.name(),
        aiSessionId,
        requestId,
        null,
        null,
        null,
        "BUNDLE-1",
        aiSessionId,
        "{}",
        "{}",
        new ObjectMapper().createObjectNode(),
        LocalDateTime.of(2026, 5, 25, 12, 0));
  }
}
