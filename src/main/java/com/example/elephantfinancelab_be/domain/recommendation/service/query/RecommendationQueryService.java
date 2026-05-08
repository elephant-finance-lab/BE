package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;

public interface RecommendationQueryService {
  RecommendationResDTO.RecommendationListDTO findRecommendationList();

  RecommendationResDTO.RecommendationDetailDTO findRecommendationDetail(String stockCode);

  Long findUserIdByEmail(String email);
}
