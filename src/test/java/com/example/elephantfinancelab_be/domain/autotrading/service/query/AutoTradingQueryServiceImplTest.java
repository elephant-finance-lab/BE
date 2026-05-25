package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
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
  void persistsCompletedStatusWhenAiReportsMatchingStoppedSession() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-1")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .aiSessionId("ai-session-1")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-1", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.getPaperAutoTradingStatus(anyString()))
        .thenReturn(
            PaperAutoTradingStatusResponse.newBuilder()
                .setSessionId("ai-session-1")
                .setStatus("IDLE")
                .setRunning(false)
                .build());
    when(sessionRepository.saveAndFlush(any(AutoTradingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AutoTradingResDTO.AiStatus result = service.findAiStatus(1L, "be-session-1");

    assertThat(result.isMatchesSession()).isTrue();
    assertThat(session.getStatus()).isEqualTo(AutoTradingSessionStatus.COMPLETED);
    verify(sessionRepository).saveAndFlush(session);
  }
}
