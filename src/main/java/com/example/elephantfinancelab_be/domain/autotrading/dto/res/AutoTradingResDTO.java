package com.example.elephantfinancelab_be.domain.autotrading.dto.res;

import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AutoTradingResDTO {

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Session {
    private String sessionId;
    private AutoTradingSessionStatus status;
    private List<String> selectedTickers;
    private List<Long> recommendationIds;
    private Integer purchaseOptionId;
    private String aiRequestId;
    private String aiSessionId;
    private String aiStatusMessage;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private LocalDateTime failedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }

  @Builder
  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AiStatus {
    private String sessionId;
    private AutoTradingSessionStatus sessionStatus;
    private String aiSessionId;
    private boolean matchesSession;
    private boolean running;
    private String status;
    private int completedCycles;
    private int totalCycles;
    private String startedAt;
    private String bundleId;
    private String lastCycleAt;
    private boolean stopRequested;
    private String terminalStatus;
    private String stopReason;
    private String endedAt;
    private String reportPath;
    private String lastError;
  }
}
