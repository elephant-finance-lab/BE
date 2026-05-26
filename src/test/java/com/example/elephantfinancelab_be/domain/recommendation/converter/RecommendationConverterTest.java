package com.example.elephantfinancelab_be.domain.recommendation.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import org.junit.jupiter.api.Test;

class RecommendationConverterTest {

  @Test
  void listItemContainsRecommendationIdSeparatelyFromRank() {
    Recommendation recommendation =
        Recommendation.builder()
            .id(42L)
            .ranking(1)
            .tickerCode("005930")
            .companyName("삼성전자")
            .recommendReason("반도체 업황 회복과 안정적인 사업 구성을 반영한 추천입니다.")
            .build();

    RecommendationResDTO.RecommendationInfoDTO result =
        RecommendationConverter.toRecommendationInfoDTO(recommendation);

    assertThat(result.getRecommendationId()).isEqualTo(42L);
    assertThat(result.getRank()).isEqualTo(1);
    assertThat(result.getTickerCode()).isEqualTo("005930");
    assertThat(result.getStockCode()).isEqualTo("005930");
    assertThat(result.getStockName()).isEqualTo("삼성전자");
    assertThat(result.getReason()).isEqualTo("반도체 업황 회복과 안정적인 사업 구성을 반영한 추천입니다.");
  }
}
