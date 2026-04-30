package com.example.elephantfinancelab_be.domain.recommendation.converter;

import com.example.elephantfinancelab_be.domain.recommendation.dto.RecommendationResponseDTO;
import java.util.List;

public class RecommendationConverter {

    // 1. 목록 조회 변환
    public static RecommendationResponseDTO.RecommendationListDTO toRecommendationListDTO(List<RecommendationResponseDTO.RecommendationInfoDTO> infoDTOList) {
        return RecommendationResponseDTO.RecommendationListDTO.builder()
                .userProfileSummary("안정형 투자자 · 변동성 낮고 배당 중심")
                .recommendations(infoDTOList)
                .build();
    }

    // 2. 상세 조회 변환
    public static RecommendationResponseDTO.RecommendationDetailDTO toRecommendationDetailDTO(String stockCode) {
        return RecommendationResponseDTO.RecommendationDetailDTO.builder()
                .recommendationId(101L)
                .tickerCode(stockCode)
                .companyName("Apple")
                .logoUrl("https://...")
                .userProfileSummary("안정형 투자자 · 변동성 낮고 배당 중심")
                .currentPrice(181200L)
                .changeRate(-4.2)
                .currency("KRW")
                .rank(1)
                .score(0.92)
                .sections(RecommendationResponseDTO.DetailSectionsDTO.builder()
                        .recommendReason("현재 시장 상황과 사용자의 투자 성향을 종합했을 때 안정적인 현금흐름과 견고한 실적을 바탕으로 장기 보유 관점에서 유리한 종목입니다.")
                        .companySummary("Apple은 글로벌 IT 기업으로 하드웨어, 소프트웨어, 서비스 생태계를 기반으로 높은 수익성과 충성 고객층을 보유하고 있습니다.")
                        .growthPoint("서비스 매출 확대와 AI 디바이스 생태계 확장 가능성이 있으며, 장기적으로 안정적인 성장 흐름이 기대됩니다.")
                        .priceAttractiveness("최근 조정 구간으로 밸류에이션 부담이 일부 완화되어 분할 매수 관점에서 접근 가능성이 있습니다.")
                        .risk("단기적으로는 미국 기술주 변동성과 환율 변화에 영향을 받을 수 있으며, 실적 발표 전후 주가 변동성이 커질 수 있습니다.")
                        .build())
                .build();
    }

    // 목록용 임시 리스트
    public static List<RecommendationResponseDTO.RecommendationInfoDTO> toRecommendationInfoDTOList() {
        return List.of(
                RecommendationResponseDTO.RecommendationInfoDTO.builder()
                        .rank(1)
                        .tickerCode("AAPL")
                        .companyName("Apple")
                        .logoUrl("https://...")
                        .currentPrice(181200L)
                        .changeRate(-4.2)
                        .currency("KRW")
                        .isSelected(true)
                        .reason("안정적인 수익 구조")
                        .score(0.92)
                        .build()
        );
    }
}