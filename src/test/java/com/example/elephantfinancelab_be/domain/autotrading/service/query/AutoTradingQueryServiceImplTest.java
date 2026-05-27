package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AutoTradingQueryServiceImplTest {

  private final AutoTradingSessionRepository sessionRepository =
      mock(AutoTradingSessionRepository.class);
  private final AiServerClient aiServerClient = mock(AiServerClient.class);
  private final AutoTradingQueryServiceImpl service =
      new AutoTradingQueryServiceImpl(sessionRepository, aiServerClient);

  @Test
  void returnsCurrentUsersActiveSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-active")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .selectedTickers("005930")
            .recommendationIds("1")
            .build();
    when(sessionRepository.findByUserIdAndActiveSlot(1L, "SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.of(session));

    AutoTradingResDTO.Session result = service.findActiveSession(1L);

    assertThat(result.getSessionId()).isEqualTo("be-session-active");
    assertThat(result.getStatus()).isEqualTo(AutoTradingSessionStatus.RUNNING);
  }

  @Test
  void returnsNullWhenCurrentUserHasNoActiveSession() {
    when(sessionRepository.findByUserIdAndActiveSlot(1L, "SHARED_KIS_VIRTUAL_ACCOUNT"))
        .thenReturn(Optional.empty());

    AutoTradingResDTO.Session result = service.findActiveSession(1L);

    assertThat(result).isNull();
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
}
