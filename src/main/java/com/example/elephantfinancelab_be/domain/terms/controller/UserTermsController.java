package com.example.elephantfinancelab_be.domain.terms.controller;

import com.example.elephantfinancelab_be.domain.terms.dto.req.UserTermsReqDTO;
import com.example.elephantfinancelab_be.domain.terms.dto.res.UserTermsResDTO;
import com.example.elephantfinancelab_be.domain.terms.service.command.UserTermsCommandService;
import com.example.elephantfinancelab_be.domain.terms.service.query.UserTermsQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Terms", description = "회원가입 약관 동의")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/terms")
public class UserTermsController {

  // TODO: 하드코딩 해제
  private static final Long DEV_USER_ID = 1L;

  private final UserTermsQueryService userTermsQueryService;
  private final UserTermsCommandService userTermsCommandService;

  @Operation(
      summary = "내 약관 동의 현황",
      description = "INVESTMENT, TRADE_RISK, PRIVACY, SERVICE 네 가지 동의 여부를 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<UserTermsResDTO.MyTerms>> getMyTerms() {
    UserTermsResDTO.MyTerms result = userTermsQueryService.getMyTerms(DEV_USER_ID);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "약관 전체 동의", description = "네 가지 약관을 한 번에 동의 처리합니다 (upsert).")
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> agreeAll(
      @Valid @RequestBody UserTermsReqDTO.AgreeAll request) {
    userTermsCommandService.agreeAll(DEV_USER_ID, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }
}
