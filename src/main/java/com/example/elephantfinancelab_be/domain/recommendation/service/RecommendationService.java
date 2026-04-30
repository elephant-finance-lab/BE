package com.example.elephantfinancelab_be.domain.recommendation.service;

import com.example.elephantfinancelab_be.domain.recommendation.converter.RecommendationConverter;
import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    public RecommendationResponseDTO.RecommendationListDTO getRecommendationList() {
        // 1. 데이터 조회 (현재는 Converter에서 임시 생성)
        var infoList = RecommendationConverter.toRecommendationInfoDTOList();

        // 2. DTO 변환 후 반환
        return RecommendationConverter.toRecommendationListDTO(infoList);
    }
    public RecommendationResponseDTO.RecommendationDetailDTO getRecommendationDetail(String stockCode) {
        // 실제로는 DB에서 stockCode로 정보를 조회해야 하지만, 현재는 임시 데이터 반환
        return RecommendationConverter.toRecommendationDetailDTO(stockCode);
    }
}