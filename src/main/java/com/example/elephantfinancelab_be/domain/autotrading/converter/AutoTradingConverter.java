package com.example.elephantfinancelab_be.domain.autotrading.converter;

import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.elephant.ai.v1.ServiceReadinessResponse;
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
        .sessionStatus(session.getStatus())
        .aiSessionId(response.getSessionId())
        .matchesSession(matchesSession)
        .running(response.getRunning())
        .status(response.getStatus())
        .completedCycles(response.getCompletedCycles())
        .totalCycles(response.getTotalCycles())
        .startedAt(response.getStartedAt())
        .bundleId(response.getBundleId())
        .lastCycleAt(response.getLastCycleAt())
        .stopRequested(response.getStopRequested())
        .terminalStatus(response.getTerminalStatus())
        .stopReason(response.getStopReason())
        .endedAt(response.getEndedAt())
        .reportPath(response.getReportPath())
        .lastError(response.getLastError())
        .build();
  }

  public static AutoTradingResDTO.Readiness toReadiness(ServiceReadinessResponse response) {
    return toReadiness(response, false, false);
  }

  public static AutoTradingResDTO.Readiness toReadiness(
      ServiceReadinessResponse response,
      boolean activeSessionExists,
      boolean activeSessionOwnedByCurrentUser) {
    if (response == null) {
      return blockedReadiness(
          "", "readiness_unavailable", activeSessionExists, activeSessionOwnedByCurrentUser);
    }
    String blockedReason =
        activeSessionExists ? "active_session_exists" : readinessBlockedReason(response);
    return AutoTradingResDTO.Readiness.builder()
        .status(response.getStatus())
        .generatedAt(response.getGeneratedAt())
        .bundleId(response.getBundleId())
        .deployQuality(response.getDeployQuality())
        .brokerEvidence(response.getBrokerEvidence())
        .liveTradingAllowed(response.getLiveTradingAllowed())
        .registryMutated(response.getRegistryMutated())
        .safeToShowDashboard(response.getSafeToShowDashboard())
        .safeToEnableOrderActions(response.getSafeToEnableOrderActions())
        .safeToEnableLiveActions(response.getSafeToEnableLiveActions())
        .activeSessionExists(activeSessionExists)
        .activeSessionOwnedByCurrentUser(activeSessionOwnedByCurrentUser)
        .canStartPaperAutoTrading(blockedReason == null)
        .blockedReason(blockedReason)
        .detailsJson(response.getDetailsJson())
        .build();
  }

  public static AutoTradingResDTO.Readiness blockedReadiness(
      String bundleId, String blockedReason) {
    return blockedReadiness(bundleId, blockedReason, false, false);
  }

  public static AutoTradingResDTO.Readiness blockedReadiness(
      String bundleId,
      String blockedReason,
      boolean activeSessionExists,
      boolean activeSessionOwnedByCurrentUser) {
    return AutoTradingResDTO.Readiness.builder()
        .status("BLOCKED")
        .bundleId(bundleId)
        .deployQuality("BLOCKED")
        .brokerEvidence("BLOCKED")
        .liveTradingAllowed(false)
        .registryMutated(false)
        .safeToShowDashboard(false)
        .safeToEnableOrderActions(false)
        .safeToEnableLiveActions(false)
        .activeSessionExists(activeSessionExists)
        .activeSessionOwnedByCurrentUser(activeSessionOwnedByCurrentUser)
        .canStartPaperAutoTrading(false)
        .blockedReason(blockedReason)
        .detailsJson("{}")
        .build();
  }

  private static String readinessBlockedReason(ServiceReadinessResponse response) {
    if (response == null) {
      return "readiness_unavailable";
    }
    if (!"PASS".equalsIgnoreCase(response.getDeployQuality())) {
      return "deploy_quality_blocked";
    }
    if (!"PASS".equalsIgnoreCase(response.getBrokerEvidence())) {
      return "broker_evidence_blocked";
    }
    if (!"PASS".equalsIgnoreCase(response.getStatus())) {
      return "readiness_status_not_pass";
    }
    if (!response.getSafeToEnableOrderActions()) {
      return "order_actions_disabled";
    }
    if (response.getLiveTradingAllowed()) {
      return "live_trading_allowed_true";
    }
    if (response.getRegistryMutated()) {
      return "registry_mutated_true";
    }
    if (response.getSafeToEnableLiveActions()) {
      return "live_action_gate_enabled";
    }
    return null;
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
