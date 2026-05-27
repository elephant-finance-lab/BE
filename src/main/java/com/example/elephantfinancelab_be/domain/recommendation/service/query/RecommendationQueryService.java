package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;

public interface RecommendationQueryService {
  default RecommendationResDTO.RecommendationListDTO findRecommendationList() {
    return findRecommendationList(null);
  }

  RecommendationResDTO.RecommendationListDTO findRecommendationList(String email);

  RecommendationResDTO.RecommendationDetailDTO findRecommendationDetail(Long recommendationId);

  RecommendationResDTO.RecommendationDetailDTO findRecommendationDetail(String stockCode);

  Long findUserIdByEmail(String email);
}
