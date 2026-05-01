package com.example.elephantfinancelab_be.domain.recommendation.service;

import com.example.elephantfinancelab_be.domain.recommendation.converter.RecommendationConverter;
import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationRequestDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.global.apiPayload.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

  private final RecommendationRepository recommendationRepository;

  public RecommendationResponseDTO.RecommendationListDTO getRecommendationList() {
    List<Recommendation> recommendations = recommendationRepository.findAll();

    List<RecommendationResponseDTO.RecommendationInfoDTO> infoList =
        recommendations.stream().map(RecommendationConverter::toRecommendationInfoDTO).toList();

    return RecommendationConverter.toRecommendationListDTO("사용자 맞춤 투자 추천 리스트", infoList);
  }

  public RecommendationResponseDTO.RecommendationDetailDTO getRecommendationDetail(
      String stockCode) {
    Recommendation recommendation =
        recommendationRepository
            .findByTickerCodeIgnoreCase(stockCode.trim())
            .orElseThrow(() -> new GeneralException(StockErrorCode.STOCK_NOT_FOUND));

    return RecommendationConverter.toRecommendationDetailDTO(recommendation, "맞춤형 투자 전략 분석");
  }
  public RecommendationResponseDTO.RecommendationSelectDTO saveSelectedRecommendations(
          RecommendationRequestDTO.SelectRecommendationDTO request) {

    List<Long> ids = request.getSelectedRecommendations().stream()
            .map(RecommendationRequestDTO.RecommendationIdDTO::getRecommendationId)
            .toList();

    if (ids.isEmpty()) {
      throw new GeneralException(RecommendationErrorCode.NO_SELECTED_RECOMMENDATION);
    }

    // TODO: 실제 저장 로직 구현 시 여기에 repository.saveAll() 추가
    return RecommendationResponseDTO.RecommendationSelectDTO.builder()
            .selectedCount(ids.size())
            .recommendationIds(ids)
            .build();
  }

  public RecommendationResponseDTO.PurchaseOptionDTO savePurchaseOption(
          RecommendationRequestDTO.PurchaseOptionRequestDTO request) {

    // TODO: 실제 선행 선택 여부 검증 로직 추가
    // TODO: DB 연동 시 optionId로 실제 데이터 조회

    return switch (request.getOptionId()) {
      case 1 -> RecommendationResponseDTO.PurchaseOptionDTO.builder()
              .optionId(1).minRate(5).maxRate(10).label("5 ~ 10%").build();
      case 2 -> RecommendationResponseDTO.PurchaseOptionDTO.builder()
              .optionId(2).minRate(10).maxRate(15).label("10 ~ 15%").build();
      case 3 -> RecommendationResponseDTO.PurchaseOptionDTO.builder()
              .optionId(3).minRate(15).maxRate(20).label("15 ~ 20%").build();
      case 4 -> RecommendationResponseDTO.PurchaseOptionDTO.builder()
              .optionId(4).minRate(20).maxRate(30).label("20 ~ 30%").build();
      default -> throw new GeneralException(RecommendationErrorCode.NO_PRIOR_SELECTION);
    };
  }

}
