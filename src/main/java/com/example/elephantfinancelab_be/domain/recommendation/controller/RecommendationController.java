package com.example.elephantfinancelab_be.domain.recommendation.controller;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.service.command.RecommendationCommandService;
import com.example.elephantfinancelab_be.domain.recommendation.service.query.RecommendationQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
  @GetMapping("/{stockCode}/reasons")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationDetailDTO>>
      getRecommendationDetail(@PathVariable(name = "stockCode") String stockCode) {
    RecommendationResDTO.RecommendationDetailDTO result =
        recommendationQueryService.findRecommendationDetail(stockCode);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 선택 저장", description = "사용자가 선택한 추천 종목을 저장합니다.")
  @PostMapping("/select")
  public ResponseEntity<ApiResponse<RecommendationResDTO.RecommendationSelectDTO>>
      createSelectedRecommendations(
          @Valid @RequestBody RecommendationReqDTO.SelectRecommendationDTO request) {
    RecommendationResDTO.RecommendationSelectDTO result =
        recommendationCommandService.saveSelectedRecommendations(request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "매수 비중 옵션 저장", description = "사용자가 선택한 매수 비중 옵션을 저장합니다.")
  @PostMapping("/purchase")
  public ResponseEntity<ApiResponse<RecommendationResDTO.PurchaseOptionDTO>> createPurchaseOption(
      @Valid @RequestBody RecommendationReqDTO.PurchaseOptionRequestDTO request) {
    RecommendationResDTO.PurchaseOptionDTO result =
        recommendationCommandService.savePurchaseOption(request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
