package com.example.elephantfinancelab_be.domain.recommendation.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "KAKAO_CLIENT_ID=test",
      "KAKAO_CLIENT_SECRET=test",
      "GOOGLE_CLIENT_ID=test",
      "GOOGLE_CLIENT_SECRET=test",
      "NAVER_CLIENT_ID=test",
      "NAVER_CLIENT_SECRET=test"
    })
@Transactional
class RecommendationSelectionPersistenceTest {

  @Autowired private RecommendationRepository recommendationRepository;
  @Autowired private UserSelectedRecommendationRepository selectedRepository;
  @Autowired private RecommendationCommandServiceImpl service;

  @Test
  void persistsSelectionUsingStockCodeWithoutPurchaseRatio() {
    Recommendation recommendation =
        recommendationRepository.save(Recommendation.builder().tickerCode("005930").build());
    RecommendationReqDTO.SelectRecommendationDTO request =
        new RecommendationReqDTO.SelectRecommendationDTO();
    ReflectionTestUtils.setField(request, "stockCode", "005930");

    service.saveSelectedRecommendations(1L, request);

    List<UserSelectedRecommendation> saved =
        selectedRepository.findAllByUserIdAndRecommendation_IdIn(
            1L, List.of(recommendation.getId()));
    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst().getRecommendation().getTickerCode()).isEqualTo("005930");
  }
}
