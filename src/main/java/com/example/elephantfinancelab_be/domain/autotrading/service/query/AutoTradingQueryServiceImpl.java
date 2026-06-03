package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.elephant.ai.v1.ServiceReadinessResponse;
import com.example.elephantfinancelab_be.domain.autotrading.converter.AutoTradingConverter;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.exception.code.AutoTradingErrorCode;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoTradingQueryServiceImpl implements AutoTradingQueryService {

  private static final String ACTIVE_SLOT = "SHARED_KIS_VIRTUAL_ACCOUNT";

  private final AutoTradingSessionRepository autoTradingSessionRepository;
  private final AiServerClient aiServerClient;

  @Value("${ai.paper-auto.bundle-id:}")
  private String bundleId;

  @Value("${auto-trading.operation.starting-timeout-minutes:15}")
  private long startingTimeoutMinutes;

  @Override
  @Transactional(readOnly = true)
  public AutoTradingResDTO.Session findActiveSession(Long userId) {
    return autoTradingSessionRepository
        .findByActiveSlot(ACTIVE_SLOT)
        .filter(session -> userId.equals(session.getUserId()))
        .map(AutoTradingConverter::toSession)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public AutoTradingResDTO.Session findSession(Long userId, String sessionId) {
    return AutoTradingConverter.toSession(getSession(userId, sessionId));
  }

  @Override
  public AutoTradingResDTO.AiStatus findAiStatus(Long userId, String sessionId) {
    AutoTradingSession session = getSession(userId, sessionId);
    if (session.getAiRequestId() == null || session.getAiRequestId().isBlank()) {
      throw new AutoTradingException(AutoTradingErrorCode.AI_REQUEST_ID_NOT_FOUND);
    }

    PaperAutoTradingStatusResponse response =
        aiServerClient.getPaperAutoTradingStatus(session.getAiRequestId());
    boolean matchesSession =
        session.getAiSessionId() != null
            && !session.getAiSessionId().isBlank()
            && session.getAiSessionId().equals(response.getSessionId());

    if (!matchesSession && canRecoverAcceptedStartingSession(session, response)) {
      session.markRunning(
          response.getSessionId(),
          "AI status: " + response.getStatus() + " (AI 세션 수락 상태 복구)",
          parseDateTime(response.getStartedAt()));
      session = autoTradingSessionRepository.saveAndFlush(session);
      matchesSession = true;
    } else if (matchesSession) {
      String message = "AI status: " + response.getStatus();
      if (!response.getRunning()) {
        reconcileTerminalStatus(session, response, message);
      } else {
        session.updateAiStatusMessage(message);
      }
      session = autoTradingSessionRepository.saveAndFlush(session);
    } else if (isMissingAiSession(response) && isExpiredStartingSession(session)) {
      session.markStartFailed("AI status: STARTING_TIMEOUT (AI 세션 수락 시간 초과)");
      session = autoTradingSessionRepository.saveAndFlush(session);
    } else if (isMissingAiSession(response) && shouldPreserveStartingSession(session)) {
      session.updateAiStatusMessage("AI status: STARTING (AI 세션 수락 대기 중)");
      session = autoTradingSessionRepository.saveAndFlush(session);
    } else if (isMissingAiSession(response) && !session.isTerminal()) {
      session.markStopped("AI status: IDLE (AI 서버에 실행 세션이 없습니다.)");
      session = autoTradingSessionRepository.saveAndFlush(session);
    }
    return AutoTradingConverter.toAiStatus(session, response, matchesSession);
  }

  @Override
  public AutoTradingResDTO.Readiness findReadiness(Long userId, String requestedBundleId) {
    String resolvedBundleId = resolveBundleId(requestedBundleId);
    ActiveSlotOccupancy occupancy = activeSlotOccupancy(userId);
    if (resolvedBundleId.isBlank()) {
      return AutoTradingConverter.blockedReadiness(
          "", "paper_bundle_id_missing", occupancy.exists(), occupancy.ownedByCurrentUser());
    }
    try {
      ServiceReadinessResponse readiness = aiServerClient.getServiceReadiness(resolvedBundleId);
      return AutoTradingConverter.toReadiness(
          readiness, occupancy.exists(), occupancy.ownedByCurrentUser());
    } catch (RuntimeException e) {
      return AutoTradingConverter.blockedReadiness(
          resolvedBundleId,
          "readiness_unavailable",
          occupancy.exists(),
          occupancy.ownedByCurrentUser());
    }
  }

  private String resolveBundleId(String requestedBundleId) {
    if (requestedBundleId != null && !requestedBundleId.isBlank()) {
      return requestedBundleId.trim();
    }
    if (bundleId == null) {
      return "";
    }
    return bundleId.trim();
  }

  private static void reconcileTerminalStatus(
      AutoTradingSession session, PaperAutoTradingStatusResponse response, String message) {
    if (isFailedStatus(response.getTerminalStatus())) {
      session.markFailed(message + ": " + response.getTerminalStatus());
    } else if (session.getStatus() == AutoTradingSessionStatus.STOPPING
        || "USER_REQUESTED".equalsIgnoreCase(response.getStopReason())
        || "STOPPED".equalsIgnoreCase(response.getTerminalStatus())) {
      session.markStopped(message);
    } else if (!session.isTerminal()) {
      session.markCompleted(message);
    }
  }

  private static boolean isMissingAiSession(PaperAutoTradingStatusResponse response) {
    return !response.getRunning()
        && response.getSessionId().isBlank()
        && "IDLE".equalsIgnoreCase(response.getStatus());
  }

  private static boolean shouldPreserveStartingSession(AutoTradingSession session) {
    return session.getStatus() == AutoTradingSessionStatus.STARTING
        && (session.getAiSessionId() == null || session.getAiSessionId().isBlank());
  }

  private static boolean canRecoverAcceptedStartingSession(
      AutoTradingSession session, PaperAutoTradingStatusResponse response) {
    return shouldPreserveStartingSession(session)
        && response.getRunning()
        && response.getSessionId() != null
        && !response.getSessionId().isBlank();
  }

  private boolean isExpiredStartingSession(AutoTradingSession session) {
    if (!shouldPreserveStartingSession(session) || session.getCreatedAt() == null) {
      return false;
    }
    long timeoutMinutes = Math.max(1, startingTimeoutMinutes);
    return session.getCreatedAt().plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
  }

  private static boolean isFailedStatus(String status) {
    return "FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
  }

  private static LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      return LocalDateTime.now();
    }
    try {
      return OffsetDateTime.parse(value).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDateTime.parse(value);
      } catch (DateTimeParseException ignoredAgain) {
        return LocalDateTime.now();
      }
    }
  }

  private AutoTradingSession getSession(Long userId, String sessionId) {
    return autoTradingSessionRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_FOUND));
  }

  private ActiveSlotOccupancy activeSlotOccupancy(Long userId) {
    return autoTradingSessionRepository
        .findByActiveSlot(ACTIVE_SLOT)
        .map(
            session ->
                new ActiveSlotOccupancy(true, userId != null && userId.equals(session.getUserId())))
        .orElseGet(() -> new ActiveSlotOccupancy(false, false));
  }

  private record ActiveSlotOccupancy(boolean exists, boolean ownedByCurrentUser) {}
}
