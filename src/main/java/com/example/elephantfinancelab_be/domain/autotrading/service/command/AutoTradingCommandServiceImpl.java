package com.example.elephantfinancelab_be.domain.autotrading.service.command;

import com.elephant.ai.v1.ServiceReadinessResponse;
import com.elephant.ai.v1.StartPaperAutoTradingResponse;
import com.elephant.ai.v1.StopPaperAutoTradingResponse;
import com.example.elephantfinancelab_be.domain.autotrading.converter.AutoTradingConverter;
import com.example.elephantfinancelab_be.domain.autotrading.dto.req.AutoTradingReqDTO;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.exception.code.AutoTradingErrorCode;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoTradingCommandServiceImpl implements AutoTradingCommandService {

  private static final String ACTIVE_SLOT = "SHARED_KIS_VIRTUAL_ACCOUNT";

  private final AutoTradingSessionRepository autoTradingSessionRepository;
  private final UserSelectedRecommendationRepository userSelectedRecommendationRepository;
  private final AiServerClient aiServerClient;

  @Value("${ai.paper-auto.bundle-id:}")
  private String bundleId;

  @Value("${ai.paper-auto.confirm-phrase:PAPER_AUTO_OK}")
  private String confirmPhrase;

  @Override
  public AutoTradingResDTO.Session startSession(
      Long userId, String idempotencyKey, AutoTradingReqDTO.StartSession request) {
    String normalizedKey = requireIdempotencyKey(idempotencyKey);
    AutoTradingSession previous =
        autoTradingSessionRepository
            .findByUserIdAndIdempotencyKey(userId, normalizedKey)
            .orElse(null);
    if (previous != null) {
      return AutoTradingConverter.toSession(previous);
    }

    List<Long> recommendationIds = request.getRecommendationIds().stream().distinct().toList();
    List<String> tickers = findSelectedTickers(userId, recommendationIds);
    validatePurchaseOption(request.getPurchaseOptionId());

    if (autoTradingSessionRepository.existsByActiveSlot(ACTIVE_SLOT)) {
      throw new AutoTradingException(AutoTradingErrorCode.ACTIVE_SESSION_EXISTS);
    }
    requirePaperAutoReadiness();

    String aiRequestId = UUID.randomUUID().toString();
    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers(String.join(",", tickers))
            .recommendationIds(
                recommendationIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .purchaseOptionId(request.getPurchaseOptionId())
            .idempotencyKey(normalizedKey)
            .aiRequestId(aiRequestId)
            .activeSlot(ACTIVE_SLOT)
            .build();

    try {
      session = autoTradingSessionRepository.saveAndFlush(session);
    } catch (DataIntegrityViolationException e) {
      return autoTradingSessionRepository
          .findByUserIdAndIdempotencyKey(userId, normalizedKey)
          .map(AutoTradingConverter::toSession)
          .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.ACTIVE_SESSION_EXISTS));
    }

    StartPaperAutoTradingResponse response;
    try {
      response =
          aiServerClient.startPaperAutoTrading(
              aiRequestId,
              bundleId,
              request.getCycles(),
              request.getIntervalSec(),
              tickers,
              confirmPhrase);
    } catch (AiServerException e) {
      session.markStartFailed(e.getCode().getMessage());
      autoTradingSessionRepository.saveAndFlush(session);
      throw e;
    }

    if (!response.getAccepted()) {
      session.markStartFailed(aiMessage(response.getStatus(), response.getReason()));
      autoTradingSessionRepository.saveAndFlush(session);
      throw new AutoTradingException(AutoTradingErrorCode.AI_START_REJECTED);
    }

    session.markRunning(
        response.getSessionId(),
        aiMessage(response.getStatus(), response.getReason()),
        parseDateTime(response.getStartedAt()));
    return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
  }

  @Override
  public AutoTradingResDTO.Session stopSession(Long userId, String sessionId) {
    AutoTradingSession session = findSession(userId, sessionId);
    if (session.isTerminal()) {
      return AutoTradingConverter.toSession(session);
    }
    if (session.getAiSessionId() == null || session.getAiSessionId().isBlank()) {
      throw new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_STOPPABLE);
    }

    AutoTradingSessionStatus previousStatus = session.getStatus();
    session.markStopping("AI stop requested");
    autoTradingSessionRepository.saveAndFlush(session);

    StopPaperAutoTradingResponse response;
    try {
      response =
          aiServerClient.stopPaperAutoTrading(
              UUID.randomUUID().toString(), session.getAiSessionId());
    } catch (AiServerException e) {
      session.restoreStatus(previousStatus, e.getCode().getMessage());
      autoTradingSessionRepository.saveAndFlush(session);
      throw e;
    }

    if (!response.getAccepted()) {
      if ("NOT_RUNNING".equalsIgnoreCase(response.getStatus())) {
        session.markStopped(aiMessage(response.getStatus(), response.getReason()));
        return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
      }
      session.restoreStatus(previousStatus, aiMessage(response.getStatus(), response.getReason()));
      autoTradingSessionRepository.saveAndFlush(session);
      throw new AutoTradingException(AutoTradingErrorCode.AI_STOP_REJECTED);
    }

    String message = aiMessage(response.getStatus(), response.getReason());
    if ("STOPPED".equalsIgnoreCase(response.getStatus())) {
      session.markStopped(message);
    } else {
      session.markStopping(message);
    }
    return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
  }

  private List<String> findSelectedTickers(Long userId, List<Long> recommendationIds) {
    if (recommendationIds.isEmpty()) {
      throw new AutoTradingException(AutoTradingErrorCode.INVALID_RECOMMENDATIONS);
    }
    List<UserSelectedRecommendation> selected =
        userSelectedRecommendationRepository.findAllByUserIdAndRecommendation_IdIn(
            userId, recommendationIds);
    Map<Long, String> tickerByRecommendationId = new LinkedHashMap<>();
    for (UserSelectedRecommendation item : selected) {
      tickerByRecommendationId.putIfAbsent(
          item.getRecommendation().getId(), item.getRecommendation().getTickerCode());
    }
    if (!tickerByRecommendationId.keySet().containsAll(recommendationIds)) {
      throw new AutoTradingException(AutoTradingErrorCode.INVALID_RECOMMENDATIONS);
    }
    if (recommendationIds.stream()
        .map(tickerByRecommendationId::get)
        .anyMatch(ticker -> ticker == null || ticker.isBlank())) {
      throw new AutoTradingException(AutoTradingErrorCode.SELECTED_TICKERS_EMPTY);
    }
    return recommendationIds.stream()
        .map(tickerByRecommendationId::get)
        .map(String::trim)
        .distinct()
        .toList();
  }

  private void requirePaperAutoReadiness() {
    ServiceReadinessResponse readiness = aiServerClient.getServiceReadiness(bundleId);
    if (readiness == null
        || !"PASS".equalsIgnoreCase(readiness.getStatus())
        || !readiness.getSafeToEnableOrderActions()
        || readiness.getLiveTradingAllowed()
        || readiness.getSafeToEnableLiveActions()) {
      throw new AutoTradingException(AutoTradingErrorCode.READINESS_GATE_BLOCKED);
    }
  }

  private static String requireIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new AutoTradingException(AutoTradingErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }
    return idempotencyKey.trim();
  }

  private static void validatePurchaseOption(Integer purchaseOptionId) {
    if (purchaseOptionId == null || purchaseOptionId < 1 || purchaseOptionId > 4) {
      throw new AutoTradingException(AutoTradingErrorCode.INVALID_PURCHASE_OPTION);
    }
  }

  private AutoTradingSession findSession(Long userId, String sessionId) {
    return autoTradingSessionRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_FOUND));
  }

  private static String aiMessage(String status, String reason) {
    if (reason == null || reason.isBlank()) {
      return status;
    }
    return status + ": " + reason;
  }

  private static LocalDateTime parseDateTime(String raw) {
    if (raw == null || raw.isBlank()) {
      return LocalDateTime.now();
    }
    try {
      return OffsetDateTime.parse(raw).toLocalDateTime();
    } catch (DateTimeParseException e) {
      return LocalDateTime.now();
    }
  }
}
