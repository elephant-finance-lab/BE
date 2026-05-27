package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elephant.ai.v1.GetRecommendationsResponse;
import com.elephant.ai.v1.RecommendationItem;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.exception.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationQueryServiceImplTest {

  private final RecommendationRepository recommendationRepository =
      mock(RecommendationRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AiServerClient aiServerClient = mock(AiServerClient.class);
  private final RecommendationQueryServiceImpl service =
      new RecommendationQueryServiceImpl(recommendationRepository, userRepository, aiServerClient);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "recommendationBundleId", "BUNDLE-TEST");
    ReflectionTestUtils.setField(service, "recommendationTopK", 10);
    ReflectionTestUtils.setField(service, "includeRecommendationDiagnostics", false);
  }

  @Test
  void fetchesModelRecommendationsAndCachesThemByRanking() {
    Recommendation existing =
        Recommendation.builder().id(1L).tickerCode("005930").companyName("기존명").build();
    RecommendationItem second =
        RecommendationItem.newBuilder()
            .setRecommendationId("MODEL-2")
            .setStockCode("000660")
            .setStockName("SK하이닉스")
            .setRanking(2)
            .setScore(0.81)
            .setReason("MODEL_RANKING_SIGNAL")
            .setRiskLevel("medium")
            .setModelVersion("v2")
            .setBundleId("BUNDLE-TEST")
            .build();
    RecommendationItem first =
        RecommendationItem.newBuilder()
            .setRecommendationId("MODEL-1")
            .setStockCode("005930")
            .setStockName("삼성전자")
            .setRanking(1)
            .setScore(0.92)
            .setReason("MODEL_RANKING_SIGNAL")
            .setExpectedReturnAvailable(false)
            .setRiskLevel("low")
            .setModelVersion("v2")
            .setBundleId("BUNDLE-TEST")
            .build();
    GetRecommendationsResponse response =
        GetRecommendationsResponse.newBuilder()
            .setStatus("PASS")
            .setReason("recommendations_ready")
            .setGeneratedAt("2026-05-26T09:10:00+09:00")
            .setBundleId("BUNDLE-TEST")
            .setModelVersion("v2")
            .setAsof("2026-05-26T09:09:00+09:00")
            .setMode("active")
            .addRecommendations(second)
            .addRecommendations(first)
            .build();
    when(aiServerClient.getRecommendations("BUNDLE-TEST", 10, false)).thenReturn(response);
    when(recommendationRepository.findByTickerCodeIgnoreCase("005930"))
        .thenReturn(Optional.of(existing));
    when(recommendationRepository.findByTickerCodeIgnoreCase("000660"))
        .thenReturn(Optional.empty());
    when(recommendationRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RecommendationResDTO.RecommendationListDTO result = service.findRecommendationList();

    assertThat(result.getModelStatus()).isEqualTo("PASS");
    assertThat(result.getRecommendations())
        .extracting(RecommendationResDTO.RecommendationInfoDTO::getStockCode)
        .containsExactly("005930", "000660");
    assertThat(result.getRecommendations().getFirst().getModelRecommendationId())
        .isEqualTo("MODEL-1");
    assertThat(result.getRecommendations().getFirst().getExpectedReturn()).isNull();
    assertThat(existing.getCompanyName()).isEqualTo("삼성전자");
    verify(aiServerClient).getRecommendations("BUNDLE-TEST", 10, false);
    verify(recommendationRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
  }

  @Test
  void failsClearlyWhenAiReturnsBlockedRecommendations() {
    GetRecommendationsResponse response =
        GetRecommendationsResponse.newBuilder()
            .setStatus("BLOCKED")
            .setReason("quant_agent_unavailable")
            .build();
    when(aiServerClient.getRecommendations("BUNDLE-TEST", 10, false)).thenReturn(response);

    assertThatThrownBy(service::findRecommendationList)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
  }

  @Test
  void findsRecommendationDetailById() {
    Recommendation recommendation =
        Recommendation.builder().id(1L).ranking(1).tickerCode("005930").companyName("삼성전자").build();
    when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

    RecommendationResDTO.RecommendationDetailDTO result = service.findRecommendationDetail(1L);

    assertThat(result.getRecommendationId()).isEqualTo(1L);
    assertThat(result.getStockCode()).isEqualTo("005930");
    verify(recommendationRepository).findById(1L);
  }
}
