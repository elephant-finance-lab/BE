package com.example.elephantfinancelab_be.domain.autotrading.service.command;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
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
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import com.example.elephantfinancelab_be.global.apiPayload.util.AiDetailSanitizer;
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
    AutoTradingResDTO.Session previous = findPreviousIdempotentSession(userId, idempotencyKey);
    if (previous != null) {
      return previous;
    }
    validatePurchaseOption(request.getPurchaseOptionId());
    validatePositive(request.getCycles(), "cycles");
    validatePositive(request.getIntervalSec(), "intervalSec");
    List<Long> recommendationIds = request.getRecommendationIds().stream().distinct().toList();
    String requestedBundleId = normalizeBundleId(request.getBundleId());
    String resolvedBundleId = resolveBundleId(requestedBundleId);
    List<String> tickers = findSelectedTickers(userId, recommendationIds, resolvedBundleId);
    return startResolvedSession(
        userId,
        idempotencyKey,
        resolvedBundleId,
        recommendationIds,
        tickers,
        request.getPurchaseOptionId(),
        request.getCycles(),
        request.getIntervalSec());
  }

  @Override
  public AutoTradingResDTO.Session startActiveUniverseSession(
      Long userId,
      String idempotencyKey,
      Integer purchaseOptionId,
      Integer cycles,
      Integer intervalSec) {
    AutoTradingResDTO.Session previous = findPreviousIdempotentSession(userId, idempotencyKey);
    if (previous != null) {
      return previous;
    }
    validatePurchaseOption(purchaseOptionId);
    validatePositive(cycles, "cycles");
    validatePositive(intervalSec, "intervalSec");
    String resolvedBundleId = resolveBundleId(null);
    return startResolvedSession(
        userId,
        idempotencyKey,
        resolvedBundleId,
        List.of(),
        List.of(),
        purchaseOptionId,
        cycles,
        intervalSec);
  }

  private AutoTradingResDTO.Session findPreviousIdempotentSession(
      Long userId, String idempotencyKey) {
    String normalizedKey = requireIdempotencyKey(idempotencyKey);
    return autoTradingSessionRepository
        .findByUserIdAndIdempotencyKey(userId, normalizedKey)
        .map(AutoTradingConverter::toSession)
        .orElse(null);
  }

  private AutoTradingResDTO.Session startResolvedSession(
      Long userId,
      String idempotencyKey,
      String resolvedBundleId,
      List<Long> recommendationIds,
      List<String> tickers,
      Integer purchaseOptionId,
      Integer cycles,
      Integer intervalSec) {
    String normalizedKey = requireIdempotencyKey(idempotencyKey);
    AutoTradingSession previous =
        autoTradingSessionRepository
            .findByUserIdAndIdempotencyKey(userId, normalizedKey)
            .orElse(null);
    if (previous != null) {
      return AutoTradingConverter.toSession(previous);
    }

    if (autoTradingSessionRepository.existsByActiveSlot(ACTIVE_SLOT)) {
      throw new AutoTradingException(AutoTradingErrorCode.ACTIVE_SESSION_EXISTS);
    }

    String aiRequestId = UUID.randomUUID().toString();
    try {
      requirePaperAutoReadiness(resolvedBundleId);
    } catch (AiServerException e) {
      persistFailedStartAttempt(
          userId,
          normalizedKey,
          recommendationIds,
          tickers,
          purchaseOptionId,
          aiRequestId,
          e.getClientMessage());
      throw e;
    } catch (AutoTradingException e) {
      persistFailedStartAttempt(
          userId,
          normalizedKey,
          recommendationIds,
          tickers,
          purchaseOptionId,
          aiRequestId,
          e.getClientMessage());
      throw e;
    }

    AutoTradingSession session =
        AutoTradingSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers(String.join(",", tickers))
            .recommendationIds(
                recommendationIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .purchaseOptionId(purchaseOptionId)
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
              aiRequestId, resolvedBundleId, cycles, intervalSec, tickers, confirmPhrase);
    } catch (AiServerException e) {
      return recoverOrPreserveAmbiguousStart(session, e);
    }

    if (!response.getAccepted()) {
      String message = aiMessage(response.getStatus(), response.getReason());
      session.markStartFailed(message);
      autoTradingSessionRepository.saveAndFlush(session);
      throw new AutoTradingException(AutoTradingErrorCode.AI_START_REJECTED, message);
    }

    session.markRunning(
        response.getSessionId(),
        aiMessage(response.getStatus(), response.getReason()),
        parseDateTime(response.getStartedAt()));
    return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
  }

  private AutoTradingResDTO.Session recoverOrPreserveAmbiguousStart(
      AutoTradingSession session, AiServerException cause) {
    if (!isAmbiguousStartException(cause)) {
      session.markStartFailed(cause.getClientMessage());
      autoTradingSessionRepository.saveAndFlush(session);
      throw cause;
    }

    try {
      PaperAutoTradingStatusResponse status =
          aiServerClient.getPaperAutoTradingStatus(session.getAiRequestId());
      if (status.getRunning() && hasText(status.getSessionId())) {
        session.markRunning(
            status.getSessionId(),
            "AI start response lost; recovered via status probe",
            parseDateTime(status.getStartedAt()));
        return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
      }
    } catch (RuntimeException ignored) {
      // Keep STARTING so the status monitor can recover or time out the ambiguous start later.
    }

    session.updateAiStatusMessage(
        "AI start response lost; status probe pending: "
            + AiDetailSanitizer.sanitize(cause.getClientMessage()));
    autoTradingSessionRepository.saveAndFlush(session);
    throw cause;
  }

  private static boolean isAmbiguousStartException(AiServerException cause) {
    return cause.getCode() == AiServerErrorCode.AI503_01
        || cause.getCode() == AiServerErrorCode.AI504_01;
  }

  private void persistFailedStartAttempt(
      Long userId,
      String normalizedKey,
      List<Long> recommendationIds,
      List<String> tickers,
      Integer purchaseOptionId,
      String aiRequestId,
      String message) {
    AutoTradingSession failedSession =
        AutoTradingSession.builder()
            .sessionId(UUID.randomUUID().toString())
            .userId(userId)
            .status(AutoTradingSessionStatus.STARTING)
            .selectedTickers(String.join(",", tickers))
            .recommendationIds(
                recommendationIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
            .purchaseOptionId(purchaseOptionId)
            .idempotencyKey(normalizedKey)
            .aiRequestId(aiRequestId)
            .activeSlot(ACTIVE_SLOT)
            .build();
    failedSession.markStartFailed(message);
    try {
      autoTradingSessionRepository.saveAndFlush(failedSession);
    } catch (DataIntegrityViolationException ignored) {
      // A concurrent idempotent request already recorded the attempt; preserve the AI error.
    }
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
      session.restoreStatus(previousStatus, e.getClientMessage());
      autoTradingSessionRepository.saveAndFlush(session);
      throw e;
    }

    if (!response.getAccepted()) {
      String message = aiMessage(response.getStatus(), response.getReason());
      if ("NOT_RUNNING".equalsIgnoreCase(response.getStatus())) {
        session.markStopped(message);
        return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
      }
      session.restoreStatus(previousStatus, message);
      autoTradingSessionRepository.saveAndFlush(session);
      throw new AutoTradingException(AutoTradingErrorCode.AI_STOP_REJECTED, message);
    }

    String message = aiMessage(response.getStatus(), response.getReason());
    if ("STOPPED".equalsIgnoreCase(response.getStatus())) {
      session.markStopped(message);
    } else {
      session.markStopping(message);
    }
    return AutoTradingConverter.toSession(autoTradingSessionRepository.saveAndFlush(session));
  }

  private List<String> findSelectedTickers(
      Long userId, List<Long> recommendationIds, String requestedBundleId) {
    if (recommendationIds.isEmpty()) {
      throw new AutoTradingException(AutoTradingErrorCode.INVALID_RECOMMENDATIONS);
    }
    List<UserSelectedRecommendation> selected =
        userSelectedRecommendationRepository.findAllByUserIdAndRecommendation_IdIn(
            userId, recommendationIds);
    Map<Long, String> tickerByRecommendationId = new LinkedHashMap<>();
    Map<Long, String> bundleByRecommendationId = new LinkedHashMap<>();
    for (UserSelectedRecommendation item : selected) {
      tickerByRecommendationId.putIfAbsent(
          item.getRecommendation().getId(), item.getRecommendation().getTickerCode());
      bundleByRecommendationId.putIfAbsent(
          item.getRecommendation().getId(), item.getRecommendation().getModelBundleId());
    }
    if (!tickerByRecommendationId.keySet().containsAll(recommendationIds)) {
      throw new AutoTradingException(AutoTradingErrorCode.INVALID_RECOMMENDATIONS);
    }
    if (recommendationIds.stream()
        .map(tickerByRecommendationId::get)
        .anyMatch(ticker -> ticker == null || ticker.isBlank())) {
      throw new AutoTradingException(AutoTradingErrorCode.SELECTED_TICKERS_EMPTY);
    }
    if (requestedBundleId != null
        && !requestedBundleId.isBlank()
        && recommendationIds.stream()
            .map(bundleByRecommendationId::get)
            .anyMatch(bundle -> bundle == null || !requestedBundleId.equals(bundle.trim()))) {
      throw new AutoTradingException(
          AutoTradingErrorCode.READINESS_GATE_BLOCKED, "recommendation_bundle_mismatch");
    }
    return recommendationIds.stream()
        .map(tickerByRecommendationId::get)
        .map(String::trim)
        .distinct()
        .toList();
  }

  private void requirePaperAutoReadiness(String resolvedBundleId) {
    if (resolvedBundleId == null || resolvedBundleId.isBlank()) {
      throw new AutoTradingException(
          AutoTradingErrorCode.READINESS_GATE_BLOCKED, "paper_bundle_id_missing");
    }
    ServiceReadinessResponse readiness = aiServerClient.getServiceReadiness(resolvedBundleId);
    String reason = paperAutoReadinessBlockReason(readiness);
    if (!reason.isBlank()) {
      throw new AutoTradingException(AutoTradingErrorCode.READINESS_GATE_BLOCKED, reason);
    }
  }

  private String normalizeBundleId(String requestedBundleId) {
    if (requestedBundleId != null && !requestedBundleId.isBlank()) {
      return requestedBundleId.trim();
    }
    return "";
  }

  private String resolveBundleId(String requestedBundleId) {
    if (requestedBundleId != null && !requestedBundleId.isBlank()) {
      return requestedBundleId;
    }
    if (bundleId == null) {
      return "";
    }
    return bundleId.trim();
  }

  private static String paperAutoReadinessBlockReason(ServiceReadinessResponse readiness) {
    if (readiness == null) {
      return "AI 서비스 준비 상태를 확인할 수 없습니다.";
    }
    List<String> blockers = new java.util.ArrayList<>();
    if (!"PASS".equalsIgnoreCase(readiness.getStatus())) {
      blockers.add("status=" + readiness.getStatus());
    }
    if (!"PASS".equalsIgnoreCase(readiness.getDeployQuality())) {
      blockers.add("deploy_quality_blocked");
    }
    if (!"PASS".equalsIgnoreCase(readiness.getBrokerEvidence())) {
      blockers.add("broker_evidence_blocked");
    }
    if (!readiness.getSafeToEnableOrderActions()) {
      blockers.add("order_actions_not_enabled");
    }
    if (readiness.getLiveTradingAllowed()) {
      blockers.add("live_trading_allowed_true");
    }
    if (readiness.getRegistryMutated()) {
      blockers.add("registry_mutated_true");
    }
    if (readiness.getSafeToEnableLiveActions()) {
      blockers.add("live_actions_enabled");
    }
    if (blockers.isEmpty()) {
      return "";
    }
    return "AI 주문 액션 게이트가 닫혀 있습니다. 사유: " + String.join(",", blockers);
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

  private static void validatePositive(Integer value, String fieldName) {
    if (value == null || value < 1) {
      throw new IllegalArgumentException(fieldName + " must be positive");
    }
  }

  private AutoTradingSession findSession(Long userId, String sessionId) {
    return autoTradingSessionRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_FOUND));
  }

  private static String aiMessage(String status, String reason) {
    String safeStatus = AiDetailSanitizer.sanitize(status);
    String safeReason = AiDetailSanitizer.sanitize(reason);
    if (safeStatus == null || safeStatus.isBlank()) {
      safeStatus = "AI_RESPONSE";
    }
    if (safeReason == null || safeReason.isBlank()) {
      return safeStatus;
    }
    return safeStatus + ": " + safeReason;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
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
