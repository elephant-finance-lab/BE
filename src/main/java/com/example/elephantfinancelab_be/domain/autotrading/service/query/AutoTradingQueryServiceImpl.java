package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.example.elephantfinancelab_be.domain.autotrading.converter.AutoTradingConverter;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.exception.AutoTradingException;
import com.example.elephantfinancelab_be.domain.autotrading.exception.code.AutoTradingErrorCode;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoTradingQueryServiceImpl implements AutoTradingQueryService {

  private static final String ACTIVE_SLOT = "SHARED_KIS_VIRTUAL_ACCOUNT";

  private final AutoTradingSessionRepository autoTradingSessionRepository;
  private final AiServerClient aiServerClient;

  @Override
  @Transactional(readOnly = true)
  public AutoTradingResDTO.Session findActiveSession(Long userId) {
    return autoTradingSessionRepository
        .findByUserIdAndActiveSlot(userId, ACTIVE_SLOT)
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

    if (matchesSession) {
      String message = "AI status: " + response.getStatus();
      if (!response.getRunning()) {
        reconcileTerminalStatus(session, response, message);
      } else {
        session.updateAiStatusMessage(message);
      }
      session = autoTradingSessionRepository.saveAndFlush(session);
    } else if (isMissingAiSession(response) && !session.isTerminal()) {
      session.markStopped("AI status: IDLE (AI 서버에 실행 세션이 없습니다.)");
      session = autoTradingSessionRepository.saveAndFlush(session);
    }
    return AutoTradingConverter.toAiStatus(session, response, matchesSession);
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

  private static boolean isFailedStatus(String status) {
    return "FAIL".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
  }

  private AutoTradingSession getSession(Long userId, String sessionId) {
    return autoTradingSessionRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_FOUND));
  }
}
