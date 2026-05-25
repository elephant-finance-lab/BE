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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoTradingQueryServiceImpl implements AutoTradingQueryService {

  private final AutoTradingSessionRepository autoTradingSessionRepository;
  private final AiServerClient aiServerClient;

  @Override
  @Transactional(readOnly = true)
  public AutoTradingResDTO.Session findSession(Long userId, String sessionId) {
    return AutoTradingConverter.toSession(getSession(userId, sessionId));
  }

  @Override
  public AutoTradingResDTO.AiStatus findAiStatus(Long userId, String sessionId) {
    AutoTradingSession session = getSession(userId, sessionId);
    PaperAutoTradingStatusResponse response =
        aiServerClient.getPaperAutoTradingStatus(UUID.randomUUID().toString());
    boolean matchesSession =
        session.getAiSessionId() != null
            && !session.getAiSessionId().isBlank()
            && session.getAiSessionId().equals(response.getSessionId());

    if (matchesSession) {
      String message = "AI status: " + response.getStatus();
      if (!response.getRunning() && session.getStatus() == AutoTradingSessionStatus.STOPPING) {
        session.markStopped(message);
      } else if (!response.getRunning()
          && session.getStatus() == AutoTradingSessionStatus.RUNNING) {
        session.markCompleted(message);
      } else {
        session.updateAiStatusMessage(message);
      }
      autoTradingSessionRepository.saveAndFlush(session);
    }
    return AutoTradingConverter.toAiStatus(session, response, matchesSession);
  }

  private AutoTradingSession getSession(Long userId, String sessionId) {
    return autoTradingSessionRepository
        .findBySessionIdAndUserId(sessionId, userId)
        .orElseThrow(() -> new AutoTradingException(AutoTradingErrorCode.SESSION_NOT_FOUND));
  }
}
