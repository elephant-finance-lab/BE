package com.example.elephantfinancelab_be.domain.autotrading.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.ServiceReadinessResponse;
import com.elephant.ai.v1.StartPaperAutoTradingResponse;
import com.elephant.ai.v1.StopPaperAutoTradingResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.req.AutoTradingReqDTO;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.exception.code.AutoTradingErrorCode;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AutoTradingCommandServiceImplTest {

  private final AutoTradingSessionRepository sessionRepository =
      mock(AutoTradingSessionRepository.class);
  private final UserSelectedRecommendationRepository selectedRepository =
      mock(UserSelectedRecommendationRepository.class);
  private final AiServerClient aiServerClient = mock(AiServerClient.class);
  private final AutoTradingCommandServiceImpl service =
      new AutoTradingCommandServiceImpl(sessionRepository, selectedRepository, aiServerClient);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "bundleId", "BUNDLE-TEST");
    ReflectionTestUtils.setField(service, "confirmPhrase", "PAPER_AUTO_OK");
    when(sessionRepository.saveAndFlush(any(AutoTradingSession.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void startsPaperAutoSessionAndStoresAiSessionId() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST")).thenReturn(paperReady());
    when(aiServerClient.startPaperAutoTrading(
            anyString(), anyString(), any(), any(), any(), anyString()))
        .thenReturn(
            StartPaperAutoTradingResponse.newBuilder()
                .setAccepted(true)
                .setStatus("STARTED")
                .setSessionId("ai-session-1")
                .setStartedAt("2026-05-25T12:00:00+09:00")
                .build());

    AutoTradingResDTO.Session result = service.startSession(1L, "idempotency-1", request());

    assertThat(result.getStatus()).isEqualTo(AutoTradingSessionStatus.RUNNING);
    assertThat(result.getAiSessionId()).isEqualTo("ai-session-1");
    assertThat(result.getSelectedTickers()).containsExactly("005930");
    verify(aiServerClient)
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
  }

  @Test
  void returnsExistingSessionForSameIdempotencyKeyWithoutStartingAiAgain() {
    AutoTradingSession existing =
        AutoTradingSession.builder()
            .sessionId("be-session-existing")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .selectedTickers("005930")
            .recommendationIds("1")
            .purchaseOptionId(2)
            .idempotencyKey("idempotency-1")
            .build();
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.of(existing));

    AutoTradingResDTO.Session result = service.startSession(1L, "idempotency-1", request());

    assertThat(result.getSessionId()).isEqualTo("be-session-existing");
    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
  }

  @Test
  void preservesFailedSessionWhenAiServerCannotBeReached() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST")).thenReturn(paperReady());
    when(aiServerClient.startPaperAutoTrading(
            anyString(), anyString(), any(), any(), any(), anyString()))
        .thenThrow(new AiServerException(AiServerErrorCode.AI503_01));

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AiServerException.class);

    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository, times(2)).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getAllValues().getLast().getStatus())
        .isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getAllValues().getLast().getAiStatusMessage())
        .contains("AI 서버에 연결할 수 없습니다.");
  }

  @Test
  void preservesFailedSessionWhenReadinessGrpcCallFailsBeforeStart() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenThrow(
            new AiServerException(
                AiServerErrorCode.AI503_01, "UNAVAILABLE", "AI server unavailable"));

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AiServerException.class)
        .hasMessageContaining("AI server unavailable");

    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getValue().getAiStatusMessage()).contains("AI server unavailable");
    assertThat(sessionCaptor.getValue().getActiveSlot()).isNull();
    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
  }

  @Test
  void blocksStartBeforeAiCallWhenReadinessDisallowsOrderActions() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setSafeToEnableOrderActions(false)
                .setLiveTradingAllowed(false)
                .setSafeToEnableLiveActions(false)
                .build());

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AutoTradingException.class)
        .satisfies(
            exception -> {
              assertThat(exception).hasMessageContaining("order_actions_not_enabled");
              assertThat(((AutoTradingException) exception).getCode())
                  .isEqualTo(AutoTradingErrorCode.READINESS_GATE_BLOCKED);
            });

    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getValue().getAiStatusMessage()).contains("order_actions_not_enabled");
    assertThat(sessionCaptor.getValue().getActiveSlot()).isNull();
  }

  @Test
  void preservesAiRejectedStatusReasonInFailureMessage() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST")).thenReturn(paperReady());
    when(aiServerClient.startPaperAutoTrading(
            anyString(), anyString(), any(), any(), any(), anyString()))
        .thenReturn(
            StartPaperAutoTradingResponse.newBuilder()
                .setAccepted(false)
                .setStatus("PAPER_START_GATE_BLOCKED")
                .setReason("broker_evidence_not_pass")
                .build());

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AutoTradingException.class)
        .hasMessageContaining("PAPER_START_GATE_BLOCKED: broker_evidence_not_pass");

    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository, times(2)).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getAllValues().getLast().getStatus())
        .isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getAllValues().getLast().getAiStatusMessage())
        .isEqualTo("PAPER_START_GATE_BLOCKED: broker_evidence_not_pass");
  }

  @Test
  void redactsAcceptedFalseStatusReasonBeforePersistingAndThrowing() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST")).thenReturn(paperReady());
    when(aiServerClient.startPaperAutoTrading(
            anyString(), anyString(), any(), any(), any(), anyString()))
        .thenReturn(
            StartPaperAutoTradingResponse.newBuilder()
                .setAccepted(false)
                .setStatus("PAPER_START_GATE_BLOCKED token=status-token")
                .setReason(
                    "broker_evidence_not_pass token=reason-token accountNumber=12345678 "
                        + "/Users/jangjaewon/Desktop/Full_Part/Elephant_Lab/.env")
                .build());

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AutoTradingException.class)
        .hasMessageContaining("PAPER_START_GATE_BLOCKED token=<redacted>")
        .hasMessageContaining("accountNumber=<redacted>")
        .hasMessageContaining("<local-path-redacted>")
        .hasMessageNotContaining("status-token")
        .hasMessageNotContaining("reason-token")
        .hasMessageNotContaining("12345678")
        .hasMessageNotContaining("/Users/jangjaewon");

    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository, times(2)).saveAndFlush(sessionCaptor.capture());
    String persistedMessage = sessionCaptor.getAllValues().getLast().getAiStatusMessage();
    assertThat(persistedMessage).contains("token=<redacted>");
    assertThat(persistedMessage).contains("accountNumber=<redacted>");
    assertThat(persistedMessage).contains("<local-path-redacted>");
    assertThat(persistedMessage)
        .doesNotContain("status-token", "reason-token", "12345678", "/Users/jangjaewon");
  }

  @Test
  void preservesGrpcAiDetailWhenStartThrowsAiServerException() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST")).thenReturn(paperReady());
    when(aiServerClient.startPaperAutoTrading(
            anyString(), anyString(), any(), any(), any(), anyString()))
        .thenThrow(
            new AiServerException(
                AiServerErrorCode.AI412_01,
                "FAILED_PRECONDITION",
                "paper_candidate_registry_not_found"));

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AiServerException.class)
        .hasMessageContaining("paper_candidate_registry_not_found");

    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository, times(2)).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getAllValues().getLast().getStatus())
        .isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getAllValues().getLast().getAiStatusMessage())
        .contains("paper_candidate_registry_not_found");
  }

  @Test
  void blocksPaperStartWhenReadinessIndicatesRegistryMutated() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setSafeToEnableOrderActions(true)
                .setLiveTradingAllowed(false)
                .setRegistryMutated(true)
                .setSafeToEnableLiveActions(false)
                .build());

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AutoTradingException.class)
        .hasMessageContaining("registry_mutated_true")
        .satisfies(
            exception ->
                assertThat(((AutoTradingException) exception).getCode())
                    .isEqualTo(AutoTradingErrorCode.READINESS_GATE_BLOCKED));

    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
    ArgumentCaptor<AutoTradingSession> sessionCaptor =
        ArgumentCaptor.forClass(AutoTradingSession.class);
    verify(sessionRepository).saveAndFlush(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(AutoTradingSessionStatus.FAILED);
    assertThat(sessionCaptor.getValue().getAiStatusMessage()).contains("registry_mutated_true");
    assertThat(sessionCaptor.getValue().getActiveSlot()).isNull();
  }

  @Test
  void blocksPaperStartWhenReadinessIndicatesLiveActions() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(sessionRepository.existsByActiveSlot(anyString())).thenReturn(false);
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930")));
    when(aiServerClient.getServiceReadiness("BUNDLE-TEST"))
        .thenReturn(
            ServiceReadinessResponse.newBuilder()
                .setStatus("PASS")
                .setSafeToEnableOrderActions(true)
                .setLiveTradingAllowed(true)
                .setSafeToEnableLiveActions(true)
                .build());

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request()))
        .isInstanceOf(AutoTradingException.class)
        .satisfies(
            exception ->
                assertThat(((AutoTradingException) exception).getCode())
                    .isEqualTo(AutoTradingErrorCode.READINESS_GATE_BLOCKED));

    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
  }

  @Test
  void rejectsStartWhenAnySelectedRecommendationHasBlankTicker() {
    when(sessionRepository.findByUserIdAndIdempotencyKey(1L, "idempotency-1"))
        .thenReturn(Optional.empty());
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(1L, 2L)))
        .thenReturn(List.of(selectedRecommendation(1L, "005930"), selectedRecommendation(2L, " ")));

    assertThatThrownBy(() -> service.startSession(1L, "idempotency-1", request(List.of(1L, 2L))))
        .isInstanceOf(AutoTradingException.class)
        .satisfies(
            exception ->
                assertThat(((AutoTradingException) exception).getCode())
                    .isEqualTo(AutoTradingErrorCode.SELECTED_TICKERS_EMPTY));

    verify(aiServerClient, never())
        .startPaperAutoTrading(anyString(), anyString(), any(), any(), any(), anyString());
  }

  @Test
  void treatsAiNotRunningStopResponseAsAlreadyStopped() {
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId("be-session-stale")
            .userId(1L)
            .status(AutoTradingSessionStatus.RUNNING)
            .aiSessionId("ai-session-stale")
            .build();
    when(sessionRepository.findBySessionIdAndUserId("be-session-stale", 1L))
        .thenReturn(Optional.of(session));
    when(aiServerClient.stopPaperAutoTrading(anyString(), anyString()))
        .thenReturn(
            StopPaperAutoTradingResponse.newBuilder()
                .setAccepted(false)
                .setStatus("NOT_RUNNING")
                .setReason("실행 중인 세션 없음")
                .build());

    AutoTradingResDTO.Session result = service.stopSession(1L, "be-session-stale");

    assertThat(result.getStatus()).isEqualTo(AutoTradingSessionStatus.STOPPED);
    assertThat(result.getAiStatusMessage()).contains("NOT_RUNNING");
    verify(sessionRepository, times(2)).saveAndFlush(session);
  }

  private static AutoTradingReqDTO.StartSession request() {
    return request(List.of(1L));
  }

  private static AutoTradingReqDTO.StartSession request(List<Long> recommendationIds) {
    AutoTradingReqDTO.StartSession request = new AutoTradingReqDTO.StartSession();
    ReflectionTestUtils.setField(request, "recommendationIds", recommendationIds);
    ReflectionTestUtils.setField(request, "purchaseOptionId", 2);
    ReflectionTestUtils.setField(request, "cycles", 3);
    ReflectionTestUtils.setField(request, "intervalSec", 10);
    return request;
  }

  private static UserSelectedRecommendation selectedRecommendation(Long id, String ticker) {
    Recommendation recommendation = Recommendation.builder().id(id).tickerCode(ticker).build();
    return UserSelectedRecommendation.builder().userId(1L).recommendation(recommendation).build();
  }

  private static ServiceReadinessResponse paperReady() {
    return ServiceReadinessResponse.newBuilder()
        .setStatus("PASS")
        .setSafeToEnableOrderActions(true)
        .setLiveTradingAllowed(false)
        .setSafeToEnableLiveActions(false)
        .build();
  }
}
