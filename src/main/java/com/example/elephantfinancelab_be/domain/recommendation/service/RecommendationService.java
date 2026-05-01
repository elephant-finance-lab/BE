package com.example.elephantfinancelab_be.domain.recommendation.service;

import com.example.elephantfinancelab_be.domain.recommendation.converter.RecommendationConverter;
import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
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
}
