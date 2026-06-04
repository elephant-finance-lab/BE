package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.elephant.ai.v1.ServiceReadinessResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AutoTradingQueryServiceImplTest {

  private final AutoTradingSessionRepository sessionRepository =
      mock(AutoTradingSessionRepository.class);
  private final AiServerClient aiServerClient = mock(AiServerClient.class);
  private final AutoTradingQueryServiceImpl service =
      new AutoTradingQueryServiceImpl(sessionRepository, aiServerClient);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "bundleId", "BUNDLE-TEST");
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());
  }

  @Test
  void returnsCurrentUsersActiveSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-active")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .selectedTickers("005930,000660")
            .recommendationIds("10,11")
            .purchaseOptionId(2)
            .build();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(session));

    AutoTradingResDTO.Session result = service.findActiveSession(1L);

    assertThat(result.getSessionId()).isEqualTo("be-session-active");
    assertThat(result.getSelectedTickers()).containsExactly("005930", "000660");
    assertThat(result.getRecommendationIds()).containsExactly(10L, 11L);
  }

  @Test
  void hidesOtherUsersActiveSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-active")
            .userId(2L)
            .status(AutoTradingSessionStatus.RUNNING)
            .selectedTickers("005930")
            .recommendationIds("10")
            .purchaseOptionId(2)
            .build();
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(session));

    AutoTradingResDTO.Session result = service.findActiveSession(1L);

    assertThat(result).isNull();
  }

  @Test
  void returnsNullWhenNoActiveSessionExists() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());

    AutoTradingResDTO.Session result = service.findActiveSession(1L);

    assertThat(result).isNull();
  }

  @Test
  void returnsPaperAutoReadinessForFeGate() {
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setGeneratedAt("2026-06-04T08:30:00+09:00")
                .setBundleId("BUNDLE-TEST")
                .setDeployQuality("PASS")
                .setBrokerEvidence("PASS")
                .setLiveTradingAllowed(false)
                .setRegistryMutated(false)
                .setSafeToShowDashboard(true)
                .setSafeToEnableOrderActions(true)
                .setSafeToEnableLiveActions(false)
                .setDetailsJson("{\"read_only\":true}")
                .build());

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isTrue();
    assertThat(result.getStatus()).isEqualTo("PASS");
    assertThat(result.getBundleId()).isEqualTo("BUNDLE-TEST");
    assertThat(result.getBlockedReason()).isNull();
    assertThat(result.isRegistryMutated()).isFalse();
    assertThat(result.isActiveSessionExists()).isFalse();
    verify(aiServerClient).getServiceReadiness("BUNDLE-TEST");
  }

  @Test
  void usesRequestedBundleIdWhenReadinessQueryProvidesOne() {
    when(aiServerClient.getServiceReadiness("BUNDLE-FE"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setBundleId("BUNDLE-FE")
                .setDeployQuality("PASS")
                .setBrokerEvidence("PASS")
                .setLiveTradingAllowed(false)
                .setRegistryMutated(false)
                .setSafeToEnableOrderActions(true)
                .setSafeToEnableLiveActions(false)
                .build());

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, " BUNDLE-FE ");

    assertThat(result.isCanStartPaperAutoTrading()).isTrue();
    assertThat(result.getBundleId()).isEqualTo("BUNDLE-FE");
    verify(aiServerClient).getServiceReadiness("BUNDLE-FE");
  }

  @Test
  void returnsBlockedReadinessWhenBundleIdIsMissing() {
    ReflectionTestUtils.setField(service, "bundleId", " ");

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isFalse();
    assertThat(result.getBlockedReason()).isEqualTo("paper_bundle_id_missing");
    verify(aiServerClient, never()).getServiceReadiness(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void blocksReadinessWhenRegistryMutated() {
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setBundleId("BUNDLE-TEST")
                .setDeployQuality("PASS")
                .setBrokerEvidence("PASS")
                .setLiveTradingAllowed(false)
                .setRegistryMutated(true)
                .setSafeToEnableOrderActions(true)
                .setSafeToEnableLiveActions(false)
                .build());

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isFalse();
    assertThat(result.getBlockedReason()).isEqualTo("registry_mutated_true");
  }

  @Test
  void readinessReturnsBlockedWhenAiServerReadinessCallFails() {
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenThrow(new AiServerException(AiServerErrorCode.AI503_01));

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isFalse();
    assertThat(result.getBundleId()).isEqualTo("BUNDLE-TEST");
    assertThat(result.getBlockedReason()).isEqualTo("readiness_unavailable");
  }

  @Test
  void readinessBlocksWhenSharedActiveSlotIsOccupiedByCurrentUser() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(activeSession(1L)));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setBundleId("BUNDLE-TEST")
                .setDeployQuality("PASS")
                .setBrokerEvidence("PASS")
                .setLiveTradingAllowed(false)
                .setRegistryMutated(false)
                .setSafeToEnableOrderActions(true)
                .setSafeToEnableLiveActions(false)
                .build());

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isFalse();
    assertThat(result.getBlockedReason()).isEqualTo("active_session_exists");
    assertThat(result.isActiveSessionExists()).isTrue();
    assertThat(result.isActiveSessionOwnedByCurrentUser()).isTrue();
  }

  @Test
  void readinessBlocksWhenSharedActiveSlotIsOccupiedByAnotherUser() {
    when(sessionRepository.findByActiveSlot("SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(activeSession(2L)));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setBundleId("BUNDLE-TEST")
                .setDeployQuality("PASS")
                .setBrokerEvidence("PASS")
                .setLiveTradingAllowed(false)
                .setRegistryMutated(false)
                .setSafeToEnableOrderActions(true)
                .setSafeToEnableLiveActions(false)
                .build());

    AutoTradingResDTO.Readiness result = service.findReadiness(1L, null);

    assertThat(result.isCanStartPaperAutoTrading()).isFalse();
    assertThat(result.getBlockedReason()).isEqualTo("active_session_exists");
    assertThat(result.isActiveSessionExists()).isTrue();
    assertThat(result.isActiveSessionOwnedByCurrentUser()).isFalse();
  }

  @Test
  void requestsAiStatusUsingStoredStartRequestId() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-1")
            .aiSessionId("ai-session-1")
            .aiRequestId("start-request-1")
            .status(AutoTradingSessionStatus.RUNNING)
            .build();
    PaperAutoTradingStatusResponse response =
        PaperAutoTradingStatusResponse.newBuilder()
            .setSessionId("ai-session-1")
            .setStatus("RUNNING")
            .setRunning(true)
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-1", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-1")).thenReturn(response);
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-1");

    assertThat(result.isMatchesSession()).isTrue();
    verify(aiServerClient).getPaperAutoTradingStatus("start-request-1");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void persistsCompletedStatusWhenAiReportsMatchingStoppedSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-1")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .aiSessionId("ai-session-1")
            .aiRequestId("start-request-1")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-1", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-1"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setSessionId("ai-session-1")
                .setStatus("IDLE")
                .setRunning(false)
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-1");

    assertThat(result.isMatchesSession()).isTrue();
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.COMPLETED);
    verify(aiServerClient).getPaperAutoTradingStatus("start-request-1");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void persistsFailedStatusWhenAiReportsMatchingFailedTerminalSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-failed")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .aiSessionId("ai-session-failed")
            .aiRequestId("start-request-failed")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-failed", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-failed"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setSessionId("ai-session-failed")
                .setStatus("IDLE")
                .setRunning(false)
                .setTerminalStatus("FAIL")
                .setLastError("PAPER_MODE_REQUIRED")
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-failed");

    assertThat(result.isMatchesSession()).isTrue();
    assertThat(result.getSessionStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(result.getTerminalStatus()).isEqualTo("FAIL");
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void closesStaleRunningSessionWhenAiServerHasNoTrackedSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-stale")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .aiSessionId("ai-session-stale")
            .aiRequestId("start-request-stale")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-stale", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-stale"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setStatus("IDLE")
                .setRunning(false)
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-stale");

    assertThat(result.isMatchesSession()).isFalse();
    assertThat(result.getSessionStatus()).isEqualTo(AutoTradingSessionStatus.STOPPED);
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.STOPPED);
    assertThat(session.getAiStatusMessage()).contains("AI 서버에 실행 세션이 없습니다.");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void preservesStartingSessionWithoutAiSessionWhenAiServerStillReportsIdle() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-starting")
            .userId(1L)
            .status(AutoTradingSessionStatus.STARTING)
            .aiRequestId("start-request-starting")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-starting", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-starting"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setStatus("IDLE")
                .setRunning(false)
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-starting");

    assertThat(result.isMatchesSession()).isFalse();
    assertThat(result.getSessionStatus()).isEqualTo(AutoTradingSessionStatus.STARTING);
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.STARTING);
    assertThat(session.getAiStatusMessage()).contains("AI 세션 수락 대기 중");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void recoversStartingSessionWhenAiServerAcceptedSessionButBeLostResponse() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-recover")
            .userId(1L)
            .status(AutoTradingSessionStatus.STARTING)
            .aiRequestId("start-request-recover")
            .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-recover", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-recover"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setSessionId("ai-session-recovered")
                .setStatus("RUNNING")
                .setRunning(true)
                .setStartedAt("2026-06-04T09:01:00+09:00")
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-recover");

    assertThat(result.isMatchesSession()).isTrue();
    assertThat(result.getSessionStatus()).isEqualTo(AutoTradingSessionStatus.RUNNING);
    assertThat(session.getAiSessionId()).isEqualTo("ai-session-recovered");
    assertThat(session.getAiStatusMessage()).contains("AI 세션 수락 상태 복구");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void expiresStartingSessionWithoutAiSessionAfterTimeout() {
    ReflectionTestUtils.setField(service, "startingTimeoutMinutes", 15L);
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-starting-timeout")
            .userId(1L)
            .status(AutoTradingSessionStatus.STARTING)
            .aiRequestId("start-request-timeout")
            .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
            .build();
    ReflectionTestUtils.setField(session, "createdAt", LocalDateTime.now().minusMinutes(30));
    when(sessionRepository.findBySessionIdAndUserId("be-session-starting-timeout", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus("start-request-timeout"))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setStatus("IDLE")
                .setRunning(false)
                .build());
    when(sessionRepository.saveAndFlush(session)).thenReturn(session);

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-starting-timeout");

    assertThat(result.getSessionStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(session.getAiStatusMessage()).contains("STARTING_TIMEOUT");
    verify(sessionRepository).saveAndFlush(session);
  }

  @Test
  void rejectsAiStatusQueryWhenStartRequestIdIsMissing() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-2")
            .status(AutoTradingSessionStatus.STARTING)
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-2", 1L))
        .thenReturn(Optional.of(session));

    assertThatThrownBy(() -> service.findAiStatus(1L, "be-session-2"))
        .isInstanceOf(AutoTradingException.class);

    verify(aiServerClient, never()).getPaperAutoTradingStatus(org.mockito.ArgumentMatchers.any());
  }

  private static AutoTradingSession activeSession(Long userId) {
    return AutoTradingSession.builder()
        .sessionId("be-session-active")
        .userId(userId)
        .status(AutoTradingSessionStatus.RUNNING)
        .selectedTickers("005930")
        .recommendationIds("10")
        .purchaseOptionId(2)
        .activeSlot("SHARED_KIS_VIRTUAL_ACCOUNT")
        .build();
  }
}
