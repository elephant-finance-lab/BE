package com.example.elephantfinancelab_be.global.config;

import com.elephant.ai.v1.AgentReportEnvelope;
import com.elephant.ai.v1.AiBeBridgeServiceGrpc;
import com.elephant.ai.v1.ExecutionFeedbackEnvelope;
import com.elephant.ai.v1.FinalDecisionEnvelope;
import com.elephant.ai.v1.GetPaperAutoTradingStatusRequest;
import com.elephant.ai.v1.GetRecommendationsRequest;
import com.elephant.ai.v1.GetRecommendationsResponse;
import com.elephant.ai.v1.HealthCheckRequest;
import com.elephant.ai.v1.HealthCheckResponse;
import com.elephant.ai.v1.InternalMessageEnvelope;
import com.elephant.ai.v1.PaperAutoTradingStatusResponse;
import com.elephant.ai.v1.PortfolioPatchEnvelope;
import com.elephant.ai.v1.ServiceReadinessRequest;
import com.elephant.ai.v1.ServiceReadinessResponse;
import com.elephant.ai.v1.StartPaperAutoTradingRequest;
import com.elephant.ai.v1.StartPaperAutoTradingResponse;
import com.elephant.ai.v1.StopPaperAutoTradingRequest;
import com.elephant.ai.v1.StopPaperAutoTradingResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import com.example.elephantfinancelab_be.global.apiPayload.util.AiDetailSanitizer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiServerClient {

  private final AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub;
  private final int timeout;
  private final int healthTimeout;
  private final int readinessTimeout;
  private final int recommendationsTimeout;
  private final int paperStartTimeout;
  private final int paperStatusTimeout;
  private final int paperStopTimeout;

  @Autowired
  public AiServerClient(
      AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub,
      @Value("${ai.server.timeout}") int timeout,
      @Value("${ai.server.timeout-health:3}") int healthTimeout,
      @Value("${ai.server.timeout-readiness:10}") int readinessTimeout,
      @Value("${ai.server.timeout-recommendations:${ai.server.recommendations-timeout:90}}")
          int recommendationsTimeout,
      @Value("${ai.server.timeout-paper-start:30}") int paperStartTimeout,
      @Value("${ai.server.timeout-paper-status:5}") int paperStatusTimeout,
      @Value("${ai.server.timeout-paper-stop:10}") int paperStopTimeout) {
    this.stub = stub;
    this.timeout = timeout;
    this.healthTimeout = healthTimeout;
    this.readinessTimeout = readinessTimeout;
    this.recommendationsTimeout = recommendationsTimeout;
    this.paperStartTimeout = paperStartTimeout;
    this.paperStatusTimeout = paperStatusTimeout;
    this.paperStopTimeout = paperStopTimeout;
  }

  AiServerClient(AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub, int timeout) {
    this(stub, timeout, timeout, timeout, timeout, timeout, timeout, timeout);
  }

  private AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stubWithDeadline() {
    return stubWithDeadline(timeout);
  }

  private AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stubWithDeadline(int seconds) {
    return stub.withDeadlineAfter(Math.max(1, seconds), TimeUnit.SECONDS);
  }

  private <T> T execute(String operation, Supplier<T> call) {
    return execute(operation, timeout, call);
  }

  private <T> T execute(String operation, int timeoutSeconds, Supplier<T> call) {
    long startedAt = System.nanoTime();
    try {
      T result = call.get();
      log.info(
          "[AI Client] gRPC success operation={}, timeoutSeconds={}, elapsedMs={}",
          operation,
          timeoutSeconds,
          elapsedMillis(startedAt));
      return result;
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e, operation, timeoutSeconds, elapsedMillis(startedAt));
    }
  }

  private void executeVoid(String operation, Runnable call) {
    execute(
        operation,
        () -> {
          call.run();
          return null;
        });
  }

  AiServerException mapToAiServerException(StatusRuntimeException e) {
    return mapToAiServerException(e, "grpc-call", timeout, 0);
  }

  private AiServerException mapToAiServerException(
      StatusRuntimeException e, String operation, int timeoutSeconds, long elapsedMs) {
    Status.Code code = e.getStatus().getCode();
    String detail = sanitizeAiDetail(e.getStatus().getDescription());
    String grpcStatusCode = code.name();
    if (code == Status.Code.DEADLINE_EXCEEDED || code == Status.Code.CANCELLED) {
      log.warn(
          "[AI Client] gRPC deadline/cancel operation={}, status={}, timeoutSeconds={}, elapsedMs={}, detail={}",
          operation,
          code,
          timeoutSeconds,
          elapsedMs,
          detail == null ? "" : detail);
    } else {
      log.error(
          "[AI Client] gRPC error operation={}, status={}, timeoutSeconds={}, elapsedMs={}, detail={}",
          operation,
          code,
          timeoutSeconds,
          elapsedMs,
          detail == null ? "" : detail);
    }
    return switch (code) {
      case INVALID_ARGUMENT ->
          new AiServerException(AiServerErrorCode.AI400_01, grpcStatusCode, detail);
      case DEADLINE_EXCEEDED ->
          new AiServerException(AiServerErrorCode.AI504_01, grpcStatusCode, detail);
      case UNAUTHENTICATED ->
          new AiServerException(AiServerErrorCode.AI401_01, grpcStatusCode, detail);
      case PERMISSION_DENIED ->
          new AiServerException(AiServerErrorCode.AI403_01, grpcStatusCode, detail);
      case NOT_FOUND -> new AiServerException(AiServerErrorCode.AI404_01, grpcStatusCode, detail);
      case INTERNAL -> new AiServerException(AiServerErrorCode.AI500_01, grpcStatusCode, detail);
      case UNAVAILABLE -> new AiServerException(AiServerErrorCode.AI503_01, grpcStatusCode, detail);
      case RESOURCE_EXHAUSTED ->
          new AiServerException(AiServerErrorCode.AI429_01, grpcStatusCode, detail);
      case UNIMPLEMENTED ->
          new AiServerException(AiServerErrorCode.AI501_01, grpcStatusCode, detail);
      case CANCELLED -> new AiServerException(AiServerErrorCode.AI400_02, grpcStatusCode, detail);
      case FAILED_PRECONDITION ->
          new AiServerException(AiServerErrorCode.AI412_01, grpcStatusCode, detail);
      default -> new AiServerException(AiServerErrorCode.AI503_01, grpcStatusCode, detail);
    };
  }

  private static long elapsedMillis(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
  }

  static String sanitizeAiDetail(String raw) {
    return AiDetailSanitizer.sanitize(raw);
  }

  public HealthCheckResponse healthCheck(String bundleId) {
    HealthCheckRequest request =
        HealthCheckRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setBundleId(bundleId != null ? bundleId : "")
            .build();
    HealthCheckResponse response =
        execute(
            "healthCheck",
            healthTimeout,
            () -> stubWithDeadline(healthTimeout).healthCheck(request));
    log.info("[AI Client] 헬스체크: {}", response.getStatus());
    return response;
  }

  public ServiceReadinessResponse getServiceReadiness(String bundleId) {
    ServiceReadinessRequest request =
        ServiceReadinessRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setBundleId(bundleId != null ? bundleId : "")
            .setIncludeDetails(true)
            .build();
    return execute(
        "getServiceReadiness",
        readinessTimeout,
        () -> stubWithDeadline(readinessTimeout).getServiceReadiness(request));
  }

  public void publishPortfolioPatch(PortfolioPatchEnvelope envelope) {
    executeVoid("publishPortfolioPatch", () -> stubWithDeadline().publishPortfolioPatch(envelope));
    log.info("[AI Client] PortfolioPatch 전송 완료: {}", envelope.getPortfolioPatchId());
  }

  public void publishFinalDecision(FinalDecisionEnvelope envelope) {
    executeVoid("publishFinalDecision", () -> stubWithDeadline().publishFinalDecision(envelope));
    log.info("[AI Client] FinalDecision 전송 완료: {}", envelope.getDecisionId());
  }

  public void publishExecutionFeedback(ExecutionFeedbackEnvelope envelope) {
    executeVoid(
        "publishExecutionFeedback", () -> stubWithDeadline().publishExecutionFeedback(envelope));
    log.info("[AI Client] ExecutionFeedback 전송 완료: {}", envelope.getOrderPlanId());
  }

  public void publishInternalMessage(InternalMessageEnvelope envelope) {
    executeVoid(
        "publishInternalMessage", () -> stubWithDeadline().publishInternalMessage(envelope));
    log.info("[AI Client] InternalMessage 전송 완료: {}", envelope.getMessageId());
  }

  public void publishAgentReport(AgentReportEnvelope envelope) {
    executeVoid("publishAgentReport", () -> stubWithDeadline().publishAgentReport(envelope));
    log.info("[AI Client] AgentReport 전송 완료: {}", envelope.getReportId());
  }

  public GetRecommendationsResponse getRecommendations(
      String bundleId, Integer topK, boolean includeDiagnostics) {
    if (topK != null && topK <= 0) {
      log.warn("[AI Client] 추천 조회 요청 실패: topK must be positive. topK={}", topK);
      throw new AiServerException(AiServerErrorCode.AI400_01);
    }

    GetRecommendationsRequest.Builder request =
        GetRecommendationsRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setBundleId(bundleId != null ? bundleId : "")
            .setIncludeDiagnostics(includeDiagnostics);
    if (topK != null) {
      request.setTopK(topK);
    }
    GetRecommendationsResponse response =
        execute(
            "getRecommendations",
            recommendationsTimeout,
            () -> stubWithDeadline(recommendationsTimeout).getRecommendations(request.build()));
    log.info(
        "[AI Client] 추천 조회 완료: status={}, count={}",
        response.getStatus(),
        response.getRecommendationsCount());
    return response;
  }

  public StartPaperAutoTradingResponse startPaperAutoTrading(
      String requestId,
      String bundleId,
      Integer cycles,
      Integer intervalSec,
      List<String> tickers,
      String confirmPhrase) {
    StartPaperAutoTradingRequest.Builder builder =
        StartPaperAutoTradingRequest.newBuilder()
            .setRequestId(requestId)
            .setBundleId(bundleId != null ? bundleId : "")
            .addAllTickers(safeTickers(tickers))
            .setConfirmPhrase(confirmPhrase != null ? confirmPhrase : "");
    if (cycles != null) {
      builder.setCycles(cycles);
    }
    if (intervalSec != null) {
      builder.setIntervalSec(intervalSec);
    }
    return execute(
        "startPaperAutoTrading",
        paperStartTimeout,
        () -> stubWithDeadline(paperStartTimeout).startPaperAutoTrading(builder.build()));
  }

  public StopPaperAutoTradingResponse stopPaperAutoTrading(String requestId, String aiSessionId) {
    StopPaperAutoTradingRequest request =
        StopPaperAutoTradingRequest.newBuilder()
            .setRequestId(requestId)
            .setSessionId(aiSessionId != null ? aiSessionId : "")
            .build();
    return execute(
        "stopPaperAutoTrading",
        paperStopTimeout,
        () -> stubWithDeadline(paperStopTimeout).stopPaperAutoTrading(request));
  }

  public PaperAutoTradingStatusResponse getPaperAutoTradingStatus(String requestId) {
    GetPaperAutoTradingStatusRequest request =
        GetPaperAutoTradingStatusRequest.newBuilder().setRequestId(requestId).build();
    return execute(
        "getPaperAutoTradingStatus",
        paperStatusTimeout,
        () -> stubWithDeadline(paperStatusTimeout).getPaperAutoTradingStatus(request));
  }

  private static List<String> safeTickers(List<String> tickers) {
    return tickers == null ? List.of() : tickers.stream().filter(Objects::nonNull).toList();
  }
}
