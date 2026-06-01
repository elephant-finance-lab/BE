package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
    ReflectionTestUtils.setField(service, "cacheReadEnabled", false);
    ReflectionTestUtils.setField(service, "cacheMaxAgeSeconds", 180L);
    ReflectionTestUtils.setField(
        service, "clock", Clock.fixed(Instant.parse("2026-06-01T01:01:00Z"), ZoneOffset.UTC));
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
  void readsCachedRecommendationsWithoutCallingAiWhenCacheReadEnabled() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation cached =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .score(0.92)
            .modelRecommendationId("MODEL-1")
            .modelBundleId("BUNDLE-TEST")
            .modelVersion("v2")
            .modelGeneratedAt("2026-06-01T10:00:00+09:00")
            .modelAsof("2026-06-01T09:59:00+09:00")
            .riskLevel("low")
            .build();
    when(recommendationRepository
            .findFirstByModelGeneratedAtIsNotNullOrderByModelGeneratedAtDescRankingAsc())
        .thenReturn(Optional.of(cached));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            "2026-06-01T10:00:00+09:00", "BUNDLE-TEST"))
        .thenReturn(List.of(cached));

    RecommendationResDTO.RecommendationListDTO result = service.findRecommendationList();

    assertThat(result.getModelStatus()).isEqualTo("PASS");
    assertThat(result.getModelReason()).isEqualTo("cached_recommendations");
    assertThat(result.getMode()).isEqualTo("cached");
    assertThat(result.getCacheAgeSec()).isEqualTo(60L);
    assertThat(result.getStale()).isFalse();
    assertThat(result.getAdvisoryOnly()).isTrue();
    assertThat(result.getSafeToEnableOrderActions()).isFalse();
    assertThat(result.getLiveTradingAllowed()).isFalse();
    assertThat(result.getRecommendations())
        .extracting(RecommendationResDTO.RecommendationInfoDTO::getStockCode)
        .containsExactly("005930");
    verify(recommendationRepository)
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            "2026-06-01T10:00:00+09:00", "BUNDLE-TEST");
    verifyNoInteractions(aiServerClient);
  }

  @Test
  void readsOnlyLatestCachedRecommendationBatch() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation latest =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .score(0.92)
            .modelRecommendationId("MODEL-1")
            .modelBundleId("BUNDLE-LATEST")
            .modelVersion("v2")
            .modelGeneratedAt("2026-06-01T10:00:00+09:00")
            .modelAsof("2026-06-01T09:59:00+09:00")
            .riskLevel("low")
            .build();
    Recommendation old =
        Recommendation.builder()
            .id(2L)
            .tickerCode("000660")
            .companyName("SK하이닉스")
            .ranking(2)
            .score(0.81)
            .modelBundleId("BUNDLE-OLD")
            .modelGeneratedAt("2026-06-01T09:55:00+09:00")
            .modelAsof("2026-06-01T09:54:00+09:00")
            .build();
    when(recommendationRepository
            .findFirstByModelGeneratedAtIsNotNullOrderByModelGeneratedAtDescRankingAsc())
        .thenReturn(Optional.of(latest));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            "2026-06-01T10:00:00+09:00", "BUNDLE-LATEST"))
        .thenReturn(List.of(latest));

    RecommendationResDTO.RecommendationListDTO result = service.findRecommendationList();

    assertThat(result.getBundleId()).isEqualTo("BUNDLE-LATEST");
    assertThat(result.getRecommendations())
        .extracting(RecommendationResDTO.RecommendationInfoDTO::getStockCode)
        .containsExactly("005930");
    assertThat(result.getRecommendations())
        .extracting(RecommendationResDTO.RecommendationInfoDTO::getStockCode)
        .doesNotContain(old.getTickerCode());
  }

  @Test
  void rejectsStaleCachedRecommendationsBeforeReturningRows() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation stale =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .modelBundleId("BUNDLE-TEST")
            .modelGeneratedAt("2026-06-01T09:55:00+09:00")
            .modelAsof("2026-06-01T09:54:00+09:00")
            .build();
    when(recommendationRepository
            .findFirstByModelGeneratedAtIsNotNullOrderByModelGeneratedAtDescRankingAsc())
        .thenReturn(Optional.of(stale));

    assertThatThrownBy(service::findRecommendationList)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);

    verify(recommendationRepository, never())
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            "2026-06-01T09:55:00+09:00", "BUNDLE-TEST");
    verifyNoInteractions(aiServerClient);
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
