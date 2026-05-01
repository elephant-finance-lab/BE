package com.example.elephantfinancelab_be.domain.recommendation.controller;

import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationRequestDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import com.example.elephantfinancelab_be.domain.recommendation.service.RecommendationService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recommendation", description = "종목 추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

  private final RecommendationService recommendationService;

  @Operation(summary = "추천 종목 목록 조회", description = "유저 맞춤형 추천 종목 리스트를 조회합니다.")
  @GetMapping("")
  public ResponseEntity<ApiResponse<RecommendationResponseDTO.RecommendationListDTO>>
      getRecommendationList() {
    RecommendationResponseDTO.RecommendationListDTO result =
        recommendationService.getRecommendationList();
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 상세 조회", description = "특정 종목의 상세 추천 사유 및 분석 내용을 조회합니다.")
  @GetMapping("/{stockCode}/reasons")
  public ResponseEntity<ApiResponse<RecommendationResponseDTO.RecommendationDetailDTO>>
      getRecommendationDetail(@PathVariable(name = "stockCode") String stockCode) {
    RecommendationResponseDTO.RecommendationDetailDTO result =
        recommendationService.getRecommendationDetail(stockCode);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "추천 종목 선택 저장", description = "사용자가 선택한 추천 종목을 저장합니다.")
  @PostMapping("/select")
  public ResponseEntity<ApiResponse<RecommendationResponseDTO.RecommendationSelectDTO>>
  createSelectedRecommendations(
          @RequestBody RecommendationRequestDTO.SelectRecommendationDTO request) {
    RecommendationResponseDTO.RecommendationSelectDTO result =
            recommendationService.saveSelectedRecommendations(request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
            .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "매수 비중 옵션 저장", description = "사용자가 선택한 매수 비중 옵션을 저장합니다.")
  @PostMapping("/purchase")
  public ResponseEntity<ApiResponse<RecommendationResponseDTO.PurchaseOptionDTO>>
  createPurchaseOption(
          @RequestBody RecommendationRequestDTO.PurchaseOptionRequestDTO request) {
    RecommendationResponseDTO.PurchaseOptionDTO result =
            recommendationService.savePurchaseOption(request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
            .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
