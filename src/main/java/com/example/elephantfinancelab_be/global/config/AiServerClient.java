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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiServerClient {

  private final AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub;
  private final int timeout;

  public AiServerClient(
      AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub,
      @Value("${ai.server.timeout}") int timeout) {
    this.stub = stub;
    this.timeout = timeout;
  }

  private AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stubWithDeadline() {
    return stub.withDeadlineAfter(timeout, TimeUnit.SECONDS);
  }

  private AiServerException mapToAiServerException(StatusRuntimeException e) {
    log.error(
        "[AI Client] gRPC 오류 - status: {}, message: {}", e.getStatus().getCode(), e.getMessage());
    Status.Code code = e.getStatus().getCode();
    return switch (code) {
      case INVALID_ARGUMENT -> new AiServerException(AiServerErrorCode.AI400_01);
      case DEADLINE_EXCEEDED -> new AiServerException(AiServerErrorCode.AI504_01);
      case UNAUTHENTICATED -> new AiServerException(AiServerErrorCode.AI401_01);
      case PERMISSION_DENIED -> new AiServerException(AiServerErrorCode.AI403_01);
      case NOT_FOUND -> new AiServerException(AiServerErrorCode.AI404_01);
      case INTERNAL -> new AiServerException(AiServerErrorCode.AI500_01);
      case UNAVAILABLE -> new AiServerException(AiServerErrorCode.AI503_01);
      case RESOURCE_EXHAUSTED -> new AiServerException(AiServerErrorCode.AI429_01);
      case UNIMPLEMENTED -> new AiServerException(AiServerErrorCode.AI501_01);
      case CANCELLED -> new AiServerException(AiServerErrorCode.AI400_02);
      case FAILED_PRECONDITION -> new AiServerException(AiServerErrorCode.AI400_01);
      default -> new AiServerException(AiServerErrorCode.AI503_01);
    };
  }

  public HealthCheckResponse healthCheck(String bundleId) {
    try {
      HealthCheckRequest request =
          HealthCheckRequest.newBuilder()
              .setRequestId(UUID.randomUUID().toString())
              .setBundleId(bundleId != null ? bundleId : "")
              .build();
      HealthCheckResponse response = stubWithDeadline().healthCheck(request);
      log.info("[AI Client] 헬스체크: {}", response.getStatus());
      return response;
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public ServiceReadinessResponse getServiceReadiness(String bundleId) {
    try {
      ServiceReadinessRequest request =
          ServiceReadinessRequest.newBuilder()
              .setRequestId(UUID.randomUUID().toString())
              .setBundleId(bundleId != null ? bundleId : "")
              .setIncludeDetails(true)
              .build();
      return stubWithDeadline().getServiceReadiness(request);
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public void publishPortfolioPatch(PortfolioPatchEnvelope envelope) {
    try {
      stubWithDeadline().publishPortfolioPatch(envelope);
      log.info("[AI Client] PortfolioPatch 전송 완료: {}", envelope.getPortfolioPatchId());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public void publishFinalDecision(FinalDecisionEnvelope envelope) {
    try {
      stubWithDeadline().publishFinalDecision(envelope);
      log.info("[AI Client] FinalDecision 전송 완료: {}", envelope.getDecisionId());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public void publishExecutionFeedback(ExecutionFeedbackEnvelope envelope) {
    try {
      stubWithDeadline().publishExecutionFeedback(envelope);
      log.info("[AI Client] ExecutionFeedback 전송 완료: {}", envelope.getOrderPlanId());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public void publishInternalMessage(InternalMessageEnvelope envelope) {
    try {
      stubWithDeadline().publishInternalMessage(envelope);
      log.info("[AI Client] InternalMessage 전송 완료: {}", envelope.getMessageId());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public void publishAgentReport(AgentReportEnvelope envelope) {
    try {
      stubWithDeadline().publishAgentReport(envelope);
      log.info("[AI Client] AgentReport 전송 완료: {}", envelope.getReportId());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public GetRecommendationsResponse getRecommendations(
      String bundleId, Integer topK, boolean includeDiagnostics) {
    if (topK != null && topK <= 0) {
      log.warn("[AI Client] 추천 조회 요청 실패: topK must be positive. topK={}", topK);
      throw new AiServerException(AiServerErrorCode.AI400_01);
    }

    try {
      GetRecommendationsRequest.Builder request =
          GetRecommendationsRequest.newBuilder()
              .setRequestId(UUID.randomUUID().toString())
              .setBundleId(bundleId != null ? bundleId : "")
              .setIncludeDiagnostics(includeDiagnostics);
      if (topK != null) {
        request.setTopK(topK);
      }
      GetRecommendationsResponse response = stubWithDeadline().getRecommendations(request.build());
      log.info(
          "[AI Client] 추천 조회 완료: status={}, count={}",
          response.getStatus(),
          response.getRecommendationsCount());
      return response;
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public StartPaperAutoTradingResponse startPaperAutoTrading(
      String requestId,
      String bundleId,
      Integer cycles,
      Integer intervalSec,
      List<String> tickers,
      String confirmPhrase) {
    try {
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
      return stubWithDeadline().startPaperAutoTrading(builder.build());
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public StopPaperAutoTradingResponse stopPaperAutoTrading(String requestId, String aiSessionId) {
    try {
      StopPaperAutoTradingRequest request =
          StopPaperAutoTradingRequest.newBuilder()
              .setRequestId(requestId)
              .setSessionId(aiSessionId != null ? aiSessionId : "")
              .build();
      return stubWithDeadline().stopPaperAutoTrading(request);
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  public PaperAutoTradingStatusResponse getPaperAutoTradingStatus(String requestId) {
    try {
      GetPaperAutoTradingStatusRequest request =
          GetPaperAutoTradingStatusRequest.newBuilder().setRequestId(requestId).build();
      return stubWithDeadline().getPaperAutoTradingStatus(request);
    } catch (StatusRuntimeException e) {
      throw mapToAiServerException(e);
    }
  }

  private static List<String> safeTickers(List<String> tickers) {
    return tickers == null ? List.of() : tickers.stream().filter(Objects::nonNull).toList();
  }
}
