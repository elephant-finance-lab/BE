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
