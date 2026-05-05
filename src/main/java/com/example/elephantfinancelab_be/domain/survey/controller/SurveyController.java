package com.example.elephantfinancelab_be.domain.survey.controller;

import com.example.elephantfinancelab_be.domain.survey.dto.req.SurveyReqDTO;
import com.example.elephantfinancelab_be.domain.survey.dto.res.SurveyResDTO;
import com.example.elephantfinancelab_be.domain.survey.service.command.SurveyCommandService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey", description = "선호도 조사 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/surveys")
public class SurveyController {

  // TODO: 하드코딩 해제
  private static final Long DEV_USER_ID = 1L;

  private final SurveyCommandService surveyCommandService;

  @Operation(
      summary = "설문 응답 제출",
      description = "투자 선호도 조사 8개 문항의 응답을 제출하고 저장합니다. 사용자마다 여러 번 제출할 수 있습니다.")
  @PostMapping("/responses")
  public ResponseEntity<ApiResponse<SurveyResDTO.SubmitResponse>> submitResponse(
      @Valid @RequestBody SurveyReqDTO.SubmitResponse request) {
    SurveyResDTO.SubmitResponse result = surveyCommandService.submitResponse(DEV_USER_ID, request);
    return ResponseEntity.status(GeneralSuccessCode.CREATED.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.CREATED, result));
  }
}
