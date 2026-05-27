package com.example.elephantfinancelab_be.domain.recommendation.controller;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.service.command.RecommendationCommandService;
import com.example.elephantfinancelab_be.domain.recommendation.service.query.RecommendationQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recommendation", description = "종목 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

  private final RecommendationQueryService recommendationQueryService;
  private final RecommendationCommandService recommendationCommandService;

  @Operation(summary = "추천 종목 목록 조회", description = "유저 맞춤형 추천 종목 리스트를 조회합니다.")
  @GetMapping("")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationListDTO>>
      getRecommendationList() {
    RecommendationResDTO.RecommendationListDTO result =
        recommendationQueryService.findRecommendationList();
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 상세 조회", description = "특정 종목의 상세 추천 사유 및 분석 내용을 조회합니다.")
  @GetMapping("/{recommendationId}")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationDetailDTO>>
      getRecommendationDetailById(
          @Parameter(description = "추천 ID", example = "1") @PathVariable(name = "recommendationId")
              Long recommendationId) {
    RecommendationResDTO.RecommendationDetailDTO result =
        recommendationQueryService.findRecommendationDetail(recommendationId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 상세 조회(종목 코드)", description = "종목 코드로 상세 추천 사유 및 분석 내용을 조회합니다.")
  @GetMapping("/{stockCode}/reasons")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationDetailDTO>>
      getRecommendationDetailByStockCode(
          @Parameter(description = "종목 코드", example = "005930") @PathVariable(name = "stockCode")
              String stockCode) {
    RecommendationResDTO.RecommendationDetailDTO result =
        recommendationQueryService.findRecommendationDetail(stockCode);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 선택 저장", description = "사용자가 선택한 추천 종목을 저장합니다.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "매수 비중 필드 없이 추천 종목 식별자만 전달합니다.",
      content =
          @Content(
              schema = @Schema(implementation = RecommendationReqDTO.SelectRecommendationDTO.class),
              examples = {
                @ExampleObject(
                    name = "single",
                    summary = "단건 선택",
                    value = "{\"recommendationId\":1,\"stockCode\":\"005930\"}"),
                @ExampleObject(
                    name = "multiple",
                    summary = "다건 선택",
                    value =
                        "{\"selectedRecommendations\":[{\"recommendationId\":1},{\"stockCode\":\"000660\"}]}")
              }))
  @PostMapping("/select")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationSelectDTO>>
      createSelectedRecommendations(
          @AuthenticationPrincipal String email,
          @Valid @RequestBody RecommendationReqDTO.SelectRecommendationDTO request) {
    Long userId = recommendationQueryService.findUserIdByEmail(email);
    RecommendationResDTO.RecommendationSelectDTO result =
        recommendationCommandService.saveSelectedRecommendations(userId, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
