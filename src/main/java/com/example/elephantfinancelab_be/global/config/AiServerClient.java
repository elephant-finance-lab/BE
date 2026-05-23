package com.example.elephantfinancelab_be.global.config;

import com.elephant.ai.v1.AgentReportEnvelope;
import com.elephant.ai.v1.AiBeBridgeServiceGrpc;
import com.elephant.ai.v1.ExecutionFeedbackEnvelope;
import com.elephant.ai.v1.FinalDecisionEnvelope;
import com.elephant.ai.v1.HealthCheckRequest;
import com.elephant.ai.v1.HealthCheckResponse;
import com.elephant.ai.v1.InternalMessageEnvelope;
import com.elephant.ai.v1.PortfolioPatchEnvelope;
import com.elephant.ai.v1.ServiceReadinessRequest;
import com.elephant.ai.v1.ServiceReadinessResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

  private final AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub stub;

  public HealthCheckResponse healthCheck(String bundleId) {
    try {
      HealthCheckRequest request =
          HealthCheckRequest.newBuilder()
              .setRequestId(UUID.randomUUID().toString())
              .setBundleId(bundleId != null ? bundleId : "")
              .build();
      HealthCheckResponse response = stub.healthCheck(request);
      log.info("[AI Client] 헬스체크: {}", response.getStatus());
      return response;
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] 헬스체크 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
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
      return stub.getServiceReadiness(request);
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] 서비스 준비 상태 확인 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }

  public void publishPortfolioPatch(PortfolioPatchEnvelope envelope) {
    try {
      stub.publishPortfolioPatch(envelope);
      log.info("[AI Client] PortfolioPatch 전송 완료: {}", envelope.getPortfolioPatchId());
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] PortfolioPatch 전송 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }

  public void publishFinalDecision(FinalDecisionEnvelope envelope) {
    try {
      stub.publishFinalDecision(envelope);
      log.info("[AI Client] FinalDecision 전송 완료: {}", envelope.getDecisionId());
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] FinalDecision 전송 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }

  public void publishExecutionFeedback(ExecutionFeedbackEnvelope envelope) {
    try {
      stub.publishExecutionFeedback(envelope);
      log.info("[AI Client] ExecutionFeedback 전송 완료: {}", envelope.getOrderPlanId());
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] ExecutionFeedback 전송 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }

  public void publishInternalMessage(InternalMessageEnvelope envelope) {
    try {
      stub.publishInternalMessage(envelope);
      log.info("[AI Client] InternalMessage 전송 완료: {}", envelope.getMessageId());
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] InternalMessage 전송 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }

  public void publishAgentReport(AgentReportEnvelope envelope) {
    try {
      stub.publishAgentReport(envelope);
      log.info("[AI Client] AgentReport 전송 완료: {}", envelope.getReportId());
    } catch (StatusRuntimeException e) {
      log.error("[AI Client] AgentReport 전송 실패: {}", e.getMessage());
      throw new AiServerException(AiServerErrorCode.AI503_01);
    }
  }
}
