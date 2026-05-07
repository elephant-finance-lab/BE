package com.example.elephantfinancelab_be.domain.recommendation.service.command;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;

public interface RecommendationCommandService {
  RecommendationResDTO.RecommendationSelectDTO saveSelectedRecommendations(
      Long userId, RecommendationReqDTO.SelectRecommendationDTO request);

  RecommendationResDTO.PurchaseOptionDTO savePurchaseOption(
      Long userId, RecommendationReqDTO.PurchaseOptionRequestDTO request);
}
