package com.example.elephantfinancelab_be.domain.autotrading.converter;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import java.util.Arrays;
import java.util.List;

public final class AutoTradingConverter {

  private AutoTradingConverter() {}

  public static AutoTradingResDTO.Session toSession(AutoTradingSession session) {
    return AutoTradingResDTO.Session.builder()
        .sessionId(session.getSessionId())
        .status(session.getStatus())
        .selectedTickers(toStringList(session.getSelectedTickers()))
        .recommendationIds(toLongList(session.getRecommendationIds()))
        .purchaseOptionId(session.getPurchaseOptionId())
        .aiRequestId(session.getAiRequestId())
        .aiSessionId(session.getAiSessionId())
        .aiStatusMessage(session.getAiStatusMessage())
        .startedAt(session.getStartedAt())
        .stoppedAt(session.getStoppedAt())
        .failedAt(session.getFailedAt())
        .createdAt(session.getCreatedAt())
        .updatedAt(session.getUpdatedAt())
        .build();
  }

  public static AutoTradingResDTO.AiStatus toAiStatus(
      AutoTradingSession session, PaperAutoTradingStatusResponse response, boolean matchesSession) {
    return AutoTradingResDTO.AiStatus.builder()
        .sessionId(session.getSessionId())
        .aiSessionId(response.getSessionId())
        .matchesSession(matchesSession)
        .running(response.getRunning())
        .status(response.getStatus())
        .completedCycles(response.getCompletedCycles())
        .totalCycles(response.getTotalCycles())
        .startedAt(response.getStartedAt())
        .bundleId(response.getBundleId())
        .lastCycleAt(response.getLastCycleAt())
        .build();
  }

  private static List<String> toStringList(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }

  private static List<Long> toLongList(String value) {
    return toStringList(value).stream().map(Long::valueOf).toList();
  }
}
