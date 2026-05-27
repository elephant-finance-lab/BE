package com.example.elephantfinancelab_be.domain.autotrading.controller;

import com.example.elephantfinancelab_be.domain.autotrading.dto.req.AutoTradingReqDTO;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.service.command.AutoTradingCommandService;
import com.example.elephantfinancelab_be.domain.autotrading.service.query.AutoTradingQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AutoTrading", description = "KIS 모의 자동매매 세션 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auto-trading/sessions")
public class AutoTradingController {

  private final AutoTradingCommandService autoTradingCommandService;
  private final AutoTradingQueryService autoTradingQueryService;
  private final UserRepository userRepository;

  @Operation(summary = "자동매매 세션 시작", description = "선택한 추천 종목으로 AI 모의 자동매매를 시작합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> startSession(
      @AuthenticationPrincipal String email,
      @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody AutoTradingReqDTO.StartSession request) {
    AutoTradingResDTO.Session result =
        autoTradingCommandService.startSession(resolveUserId(email), idempotencyKey, request);
    return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.CREATED, result));
  }

  @Operation(summary = "자동매매 세션 중지", description = "AI에 모의 자동매매 세션 중지를 요청합니다.")
  @PostMapping("/{sessionId}/stop")
  public ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> stopSession(
      @AuthenticationPrincipal String email, @PathVariable String sessionId) {
    AutoTradingResDTO.Session result =
        autoTradingCommandService.stopSession(resolveUserId(email), sessionId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "실행 중인 자동매매 세션 조회", description = "로그인 사용자의 활성 paper-auto 세션을 조회합니다.")
  @GetMapping("/active")
  public ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> getActiveSession(
      @AuthenticationPrincipal String email) {
    AutoTradingResDTO.Session result =
        autoTradingQueryService.findActiveSession(resolveUserId(email));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "자동매매 세션 조회", description = "BE DB에 저장된 세션 상태를 조회합니다.")
  @GetMapping("/{sessionId}")
  public ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> getSession(
      @AuthenticationPrincipal String email, @PathVariable String sessionId) {
    AutoTradingResDTO.Session result =
        autoTradingQueryService.findSession(resolveUserId(email), sessionId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "AI 자동매매 상태 조회", description = "AI gRPC 서버의 현재 paper-auto 실행 상태를 조회합니다.")
  @GetMapping("/{sessionId}/ai-status")
  public ResponseEntity<ApiResponse<AutoTradingResDTO.AiStatus>> getAiStatus(
      @AuthenticationPrincipal String email, @PathVariable String sessionId) {
    AutoTradingResDTO.AiStatus result =
        autoTradingQueryService.findAiStatus(resolveUserId(email), sessionId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  private Long resolveUserId(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }
}
