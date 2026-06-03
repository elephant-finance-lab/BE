package com.example.elephantfinancelab_be.domain.autotrading.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.ServiceReadinessResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingEventRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.autotrading.service.command.AutoTradingCommandService;
import com.example.elephantfinancelab_be.domain.autotrading.service.query.AutoTradingQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AutoTradingOperationSchedulerTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final AutoTradingCommandService commandService = mock(AutoTradingCommandService.class);
  private final AutoTradingQueryService queryService = mock(AutoTradingQueryService.class);
  private final AutoTradingSessionRepository sessionRepository =
      mock(AutoTradingSessionRepository.class);
  private final AutoTradingEventRepository eventRepository = mock(AutoTradingEventRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AiServerClient aiServerClient = mock(AiServerClient.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AutoTradingOperationScheduler scheduler =
      scheduler(Clock.fixed(Instant.parse("2026-06-04T00:01:00Z"), KST));

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(scheduler, "operationEnabled", true);
    ReflectionTestUtils.setField(scheduler, "operatorEmail", "operator@example.com");
    ReflectionTestUtils.setField(scheduler, "dryRun", false);
    ReflectionTestUtils.setField(scheduler, "purchaseOptionId", 2);
    ReflectionTestUtils.setField(scheduler, "cycles", 390);
    ReflectionTestUtils.setField(scheduler, "intervalSec", 60);
    ReflectionTestUtils.setField(scheduler, "retryAttempts", 2);
    ReflectionTestUtils.setField(scheduler, "retryBackoffMs", 0L);
    ReflectionTestUtils.setField(scheduler, "startingTimeoutMinutes", 15L);
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");
    ReflectionTestUtils.setField(scheduler, "bundleId", "BUNDLE-TEST");
  }

  @Test
  void startDailyPaperAutoUsesActiveUniverseWhenEnabled() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail("operator@example.com")).thenReturn(Optional.of(user(7L)));
    when(commandService.startActiveUniverseSession(7L, "server-paper-auto-2026-06-04", 2, 390, 60))
        .thenReturn(
            AutoTradingResDTO.Session.builder()
                .sessionId("be-session-1")
                .status(AutoTradingSessionStatus.RUNNING)
                .build());

    scheduler.startDailyPaperAuto();

    verify(commandService)
        .startActiveUniverseSession(7L, "server-paper-auto-2026-06-04", 2, 390, 60);
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
  }

  @Test
  void startDailyPaperAutoDryRunAuditsWithoutStarting() {
    ReflectionTestUtils.setField(scheduler, "dryRun", true);
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail("operator@example.com")).thenReturn(Optional.of(user(7L)));

    scheduler.startDailyPaperAuto();

    verify(commandService, never())
        .startActiveUniverseSession(anyLong(), anyString(), any(), any(), any());
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("dry_run_enabled");
  }

  @Test
  void startDailyPaperAutoFailsClosedWhenBundleIdMissing() {
    ReflectionTestUtils.setField(scheduler, "bundleId", " ");

    scheduler.startDailyPaperAuto();

    verifyNoInteractions(commandService, userRepository);
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("paper_bundle_id_missing");
  }

  @Test
  void startDailyPaperAutoDoesNotRetryStartFailureWithSameIdempotencyKey() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail("operator@example.com")).thenReturn(Optional.of(user(7L)));
    when(commandService.startActiveUniverseSession(7L, "server-paper-auto-2026-06-04", 2, 390, 60))
        .thenThrow(new IllegalStateException("temporary_ai_start_failure"));

    scheduler.startDailyPaperAuto();

    verify(commandService)
        .startActiveUniverseSession(7L, "server-paper-auto-2026-06-04", 2, 390, 60);
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("temporary_ai_start_failure");
  }

  @Test
  void startSkipsWhenOperationDisabled() {
    ReflectionTestUtils.setField(scheduler, "operationEnabled", false);

    scheduler.startDailyPaperAuto();

    verifyNoInteractions(commandService, userRepository, sessionRepository);
  }

  @Test
  void startSkipsConfiguredMarketHoliday() {
    AutoTradingOperationScheduler holidayScheduler =
        scheduler(Clock.fixed(Instant.parse("2026-06-03T00:01:00Z"), KST));
    ReflectionTestUtils.setField(holidayScheduler, "operationEnabled", true);
    ReflectionTestUtils.setField(holidayScheduler, "marketHolidays", "2026-06-03,2026-07-17");

    holidayScheduler.startDailyPaperAuto();

    verifyNoInteractions(commandService, userRepository, sessionRepository);
    assertThat(holidayScheduler.isTradingDay(LocalDate.of(2026, 6, 3))).isFalse();
  }

  @Test
  void startFailsClosedWhenMarketHolidayConfigIsMalformed() {
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,not-a-date");

    scheduler.startDailyPaperAuto();

    verifyNoInteractions(commandService, userRepository, aiServerClient);
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson())
        .contains("invalid_market_holiday_config:not-a-date");
  }

  @Test
  void preOpenReadinessCheckIsReadOnly() {
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setSafeToEnableOrderActions(true)
                .setLiveTradingAllowed(false)
                .setSafeToEnableLiveActions(false)
                .setRegistryMutated(false)
                .build());

    scheduler.runPreOpenReadinessCheck();

    verify(aiServerClient).getServiceReadiness("BUNDLE-TEST");
    verifyNoInteractions(commandService);
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
  }

  @Test
  void schedulerOperationLockSkipsOverlappingStart() {
    ReflectionTestUtils.setField(scheduler, "retryAttempts", 1);
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());
    when(userRepository.findByEmail("operator@example.com")).thenReturn(Optional.of(user(7L)));
    when(commandService.startActiveUniverseSession(7L, "server-paper-auto-2026-06-04", 2, 390, 60))
        .thenAnswer(
            invocation -> {
              scheduler.runPreOpenReadinessCheck();
              return AutoTradingResDTO.Session.builder()
                  .sessionId("be-session-1")
                  .status(AutoTradingSessionStatus.RUNNING)
                  .build();
            });

    scheduler.startDailyPaperAuto();

    verify(aiServerClient, never()).getServiceReadiness(anyString());
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository, times(2)).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getAllValues().stream().map(AutoTradingEvent::getPayloadJson))
        .anyMatch(payload -> payload.contains("operation_lock_busy"));
  }

  @Test
  void monitorReconcilesActiveSessionThroughQueryService() {
    AutoTradingSession activeSession = activeSession();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(activeSession));
    when(queryService.findAiStatus(7L, "be-session-active"))
        .thenReturn(
            AutoTradingResDTO.AiStatus.builder()
                .sessionId("be-session-active")
                .sessionStatus(AutoTradingSessionStatus.RUNNING)
                .status("RUNNING")
                .completedCycles(3)
                .totalCycles(390)
                .build());

    scheduler.monitorActivePaperAuto();

    verify(queryService).findAiStatus(7L, "be-session-active");
  }

  @Test
  void monitorSkipsStartingSessionBeforeAiSessionIdIsAccepted() {
    AutoTradingSession startingSession =
        AutoTradingSession.builder()
            .sessionId("be-session-starting")
            .userId(7L)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers("")
            .recommendationIds("")
            .purchaseOptionId(2)
            .idempotencyKey("server-paper-auto-2026-06-04")
            .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
            .aiRequestId("ai-request-1")
            .build();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(startingSession));
    when(queryService.findAiStatus(7L, "be-session-starting"))
        .thenReturn(
            AutoTradingResDTO.AiStatus.builder()
                .sessionId("be-session-starting")
                .sessionStatus(AutoTradingSessionStatus.STARTING)
                .status("IDLE")
                .running(false)
                .build());

    scheduler.monitorActivePaperAuto();

    verify(queryService).findAiStatus(7L, "be-session-starting");
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("starting_without_ai_session");
  }

  @Test
  void monitorAllowsQueryServiceToRecoverStartingSession() {
    AutoTradingSession startingSession =
        AutoTradingSession.builder()
            .sessionId("be-session-starting-recovered")
            .userId(7L)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers("")
            .recommendationIds("")
            .purchaseOptionId(2)
            .idempotencyKey("server-paper-auto-2026-06-04")
            .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
            .aiRequestId("ai-request-1")
            .build();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(startingSession));
    when(queryService.findAiStatus(7L, "be-session-starting-recovered"))
        .thenReturn(
            AutoTradingResDTO.AiStatus.builder()
                .sessionId("be-session-starting-recovered")
                .sessionStatus(AutoTradingSessionStatus.RUNNING)
                .status("RUNNING")
                .running(true)
                .build());

    scheduler.monitorActivePaperAuto();

    verify(queryService).findAiStatus(7L, "be-session-starting-recovered");
    verify(sessionRepository, never()).saveAndFlush(startingSession);
  }

  @Test
  void monitorFailsStaleStartingSessionAfterTimeout() {
    AutoTradingSession startingSession =
        AutoTradingSession.builder()
            .sessionId("be-session-starting-timeout")
            .userId(7L)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers("")
            .recommendationIds("")
            .purchaseOptionId(2)
            .idempotencyKey("server-paper-auto-2026-06-04")
            .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
            .aiRequestId("ai-request-1")
            .build();
    ReflectionTestUtils.setField(startingSession, "createdAt", LocalDateTime.of(2026, 6, 4, 8, 40));
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(startingSession));
    when(sessionRepository.saveAndFlush(startingSession)).thenReturn(startingSession);
    when(queryService.findAiStatus(7L, "be-session-starting-timeout"))
        .thenReturn(
            AutoTradingResDTO.AiStatus.builder()
                .sessionId("be-session-starting-timeout")
                .sessionStatus(AutoTradingSessionStatus.STARTING)
                .status("IDLE")
                .running(false)
                .build());

    scheduler.monitorActivePaperAuto();

    verify(queryService).findAiStatus(7L, "be-session-starting-timeout");
    verify(sessionRepository).saveAndFlush(startingSession);
    assertThat(startingSession.getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository, times(2)).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getAllValues().stream().map(AutoTradingEvent::getPayloadJson))
        .anyMatch(payload -> payload.contains("starting_session_timeout"));
  }

  @Test
  void stopDailyPaperAutoStopsActiveSession() {
    AutoTradingSession activeSession = activeSession();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(activeSession));
    when(commandService.stopSession(7L, "be-session-active"))
        .thenReturn(
            AutoTradingResDTO.Session.builder()
                .sessionId("be-session-active")
                .status(AutoTradingSessionStatus.STOPPED)
                .build());

    scheduler.stopDailyPaperAuto();

    verify(commandService).stopSession(7L, "be-session-active");
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
  }

  @Test
  void stopDailyPaperAutoDryRunAuditsWithoutStopping() {
    ReflectionTestUtils.setField(scheduler, "dryRun", true);
    AutoTradingSession activeSession = activeSession();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(activeSession));

    scheduler.stopDailyPaperAuto();

    verify(commandService, never()).stopSession(anyLong(), anyString());
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("dry_run_enabled");
  }

  @Test
  void stopDailyPaperAutoAuditsUnexpectedOuterFailure() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenThrow(new IllegalStateException("database_unavailable"));

    scheduler.stopDailyPaperAuto();

    verify(commandService, never()).stopSession(anyLong(), anyString());
    ArgumentCaptor<AutoTradingEvent> eventCaptor = ArgumentCaptor.forClass(AutoTradingEvent.class);
    verify(eventRepository).saveAndFlush(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getPayloadJson()).contains("database_unavailable");
  }

  @Test
  void stopDoesNothingWhenNoActiveSession() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());

    scheduler.stopDailyPaperAuto();

    verify(commandService, never()).stopSession(anyLong(), anyString());
    verify(eventRepository).saveAndFlush(any(AutoTradingEvent.class));
  }

  private AutoTradingOperationScheduler scheduler(Clock clock) {
    return new AutoTradingOperationScheduler(
        commandService,
        queryService,
        sessionRepository,
        userRepository,
        aiServerClient,
        eventRepository,
        objectMapper,
        clock);
  }

  private static User user(Long id) {
    return User.builder().id(id).email("operator@example.com").build();
  }

  private static AutoTradingSession activeSession() {
    return AutoTradingSession.builder()
        .sessionId("be-session-active")
        .userId(7L)
        .status(AutoTradingSessionStatus.RUNNING)
        .selectedTickers("")
        .recommendationIds("")
        .purchaseOptionId(2)
        .idempotencyKey("server-paper-auto-2026-06-04")
        .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
        .aiRequestId("ai-request-1")
        .aiSessionId("ai-session-1")
        .build();
  }
}
