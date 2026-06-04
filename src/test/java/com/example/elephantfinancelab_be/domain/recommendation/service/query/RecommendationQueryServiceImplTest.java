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
import java.time.OffsetDateTime;
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
    ReflectionTestUtils.setField(service, "cacheDisplayMaxAgeSeconds", 86_400L);
    ReflectionTestUtils.setField(service, "allowStaleDisplay", true);
    ReflectionTestUtils.setField(service, "cacheMaxFutureSkewSeconds", 5L);
    ReflectionTestUtils.setField(
        service, "clock", Clock.fixed(Instant.parse("2026-06-01T01:01:00Z"), ZoneOffset.UTC));
  }

  private static OffsetDateTime generatedAt(String raw) {
    return OffsetDateTime.parse(raw);
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
    assertThat(result.getGeneratedAt()).isEqualTo("2026-05-26T09:10:00+09:00");
    assertThat(result.getRecommendations())
        .extracting(RecommendationResDTO.RecommendationInfoDTO::getStockCode)
        .containsExactly("005930", "000660");
    assertThat(result.getRecommendations().getFirst().getModelRecommendationId())
        .isEqualTo("MODEL-1");
    assertThat(result.getRecommendations().getFirst().getExpectedReturn()).isNull();
    assertThat(existing.getCompanyName()).isEqualTo("삼성전자");
    assertThat(existing.getModelGeneratedAt()).isEqualTo(generatedAt("2026-05-26T09:10:00+09:00"));
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
            .modelGeneratedAt(generatedAt("2026-06-01T10:00:00+09:00"))
            .modelAsof("2026-06-01T09:59:00+09:00")
            .riskLevel("low")
            .build();
    when(recommendationRepository.findByModelGeneratedAtIsNotNull()).thenReturn(List.of(cached));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T10:00:00+09:00"), "BUNDLE-TEST"))
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
            generatedAt("2026-06-01T10:00:00+09:00"), "BUNDLE-TEST");
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
            .modelGeneratedAt(generatedAt("2026-06-01T10:00:00+09:00"))
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
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .modelAsof("2026-06-01T09:54:00+09:00")
            .build();
    when(recommendationRepository.findByModelGeneratedAtIsNotNull())
        .thenReturn(List.of(old, latest));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T10:00:00+09:00"), "BUNDLE-LATEST"))
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
  void selectsLatestCachedBatchByParsedTimestampInsteadOfStringOrder() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation stringSortedFirstButOlder =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .modelBundleId("BUNDLE-KST-OLDER")
            .modelGeneratedAt(generatedAt("2026-06-01T10:30:00+09:00"))
            .modelAsof("2026-06-01T10:29:00+09:00")
            .build();
    Recommendation actuallyLatest =
        Recommendation.builder()
            .id(2L)
            .tickerCode("000660")
            .companyName("SK하이닉스")
            .ranking(1)
            .modelBundleId("BUNDLE-UTC-LATEST")
            .modelGeneratedAt(generatedAt("2026-06-01T02:00:00Z"))
            .modelAsof("2026-06-01T01:59:00Z")
            .build();
    ReflectionTestUtils.setField(
        service, "clock", Clock.fixed(Instant.parse("2026-06-01T02:01:00Z"), ZoneOffset.UTC));
    when(recommendationRepository.findByModelGeneratedAtIsNotNull())
        .thenReturn(List.of(stringSortedFirstButOlder, actuallyLatest));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T02:00:00Z"), "BUNDLE-UTC-LATEST"))
        .thenReturn(List.of(actuallyLatest));

    RecommendationResDTO.RecommendationListDTO result = service.findRecommendationList();

    assertThat(result.getBundleId()).isEqualTo("BUNDLE-UTC-LATEST");
    assertThat(result.getGeneratedAt()).isEqualTo("2026-06-01T02:00:00Z");
    verify(recommendationRepository)
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T02:00:00Z"), "BUNDLE-UTC-LATEST");
    verify(recommendationRepository, never())
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T10:30:00+09:00"), "BUNDLE-KST-OLDER");
  }

  @Test
  void rejectsAiResponseWithInvalidGeneratedAtBeforeSaving() {
    GetRecommendationsResponse response =
        GetRecommendationsResponse.newBuilder()
            .setStatus("PASS")
            .setReason("recommendations_ready")
            .setGeneratedAt("not-a-timestamp")
            .setBundleId("BUNDLE-TEST")
            .setModelVersion("v2")
            .addRecommendations(
                RecommendationItem.newBuilder()
                    .setRecommendationId("MODEL-1")
                    .setStockCode("005930")
                    .setStockName("삼성전자")
                    .setRanking(1)
                    .setScore(0.92)
                    .setReason("MODEL_RANKING_SIGNAL")
                    .setRiskLevel("low")
                    .build())
            .build();
    when(aiServerClient.getRecommendations("BUNDLE-TEST", 10, false)).thenReturn(response);

    assertThatThrownBy(service::findRecommendationList)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);

    verify(recommendationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
  }

  @Test
  void returnsStaleCachedRecommendationsForDisplayWithoutCallingAi() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation stale =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .modelBundleId("BUNDLE-TEST")
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .modelAsof("2026-06-01T09:54:00+09:00")
            .build();
    when(recommendationRepository.findByModelGeneratedAtIsNotNull()).thenReturn(List.of(stale));
    when(recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T09:55:00+09:00"), "BUNDLE-TEST"))
        .thenReturn(List.of(stale));

    RecommendationResDTO.RecommendationListDTO result = service.findRecommendationList();

    assertThat(result.getModelReason()).isEqualTo("cached_recommendations");
    assertThat(result.getMode()).isEqualTo("cached");
    assertThat(result.getStale()).isTrue();
    assertThat(result.getStaleReason()).isEqualTo("cache_stale");
    assertThat(result.getSafeToEnableOrderActions()).isFalse();
    assertThat(result.getLiveTradingAllowed()).isFalse();
    verify(recommendationRepository)
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T09:55:00+09:00"), "BUNDLE-TEST");
    verifyNoInteractions(aiServerClient);
  }

  @Test
  void rejectsCachedRecommendationsBeyondDisplayWindow() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    ReflectionTestUtils.setField(service, "cacheDisplayMaxAgeSeconds", 120L);
    Recommendation stale =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .modelBundleId("BUNDLE-TEST")
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .modelAsof("2026-06-01T09:54:00+09:00")
            .build();
    when(recommendationRepository.findByModelGeneratedAtIsNotNull()).thenReturn(List.of(stale));

    assertThatThrownBy(service::findRecommendationList)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);

    verify(recommendationRepository, never())
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T09:55:00+09:00"), "BUNDLE-TEST");
    verifyNoInteractions(aiServerClient);
  }

  @Test
  void rejectsFutureCachedRecommendationsBeyondClockSkew() {
    ReflectionTestUtils.setField(service, "cacheReadEnabled", true);
    Recommendation future =
        Recommendation.builder()
            .id(1L)
            .tickerCode("005930")
            .companyName("삼성전자")
            .ranking(1)
            .modelBundleId("BUNDLE-FUTURE")
            .modelGeneratedAt(generatedAt("2026-06-01T01:02:00Z"))
            .modelAsof("2026-06-01T01:01:00Z")
            .build();
    when(recommendationRepository.findByModelGeneratedAtIsNotNull()).thenReturn(List.of(future));

    assertThatThrownBy(service::findRecommendationList)
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);

    verify(recommendationRepository, never())
        .findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            generatedAt("2026-06-01T01:02:00Z"), "BUNDLE-FUTURE");
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
        Recommendation.builder()
            .id(1L)
            .ranking(1)
            .tickerCode("005930")
            .companyName("삼성전자")
            .modelGeneratedAt(generatedAt("2026-06-01T10:00:00+09:00"))
            .build();
    when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

    RecommendationResDTO.RecommendationDetailDTO result = service.findRecommendationDetail(1L);

    assertThat(result.getRecommendationId()).isEqualTo(1L);
    assertThat(result.getStockCode()).isEqualTo("005930");
    assertThat(result.getCacheAgeSec()).isEqualTo(60L);
    assertThat(result.getStale()).isFalse();
    assertThat(result.getStaleReason()).isNull();
    assertThat(result.getAdvisoryOnly()).isTrue();
    assertThat(result.getSafeToEnableOrderActions()).isFalse();
    assertThat(result.getLiveTradingAllowed()).isFalse();
    verify(recommendationRepository).findById(1L);
  }

  @Test
  void marksRecommendationDetailAsStaleWhenGeneratedAtExceedsFreshWindow() {
    Recommendation recommendation =
        Recommendation.builder()
            .id(1L)
            .ranking(1)
            .tickerCode("005930")
            .companyName("삼성전자")
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .build();
    when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

    RecommendationResDTO.RecommendationDetailDTO result = service.findRecommendationDetail(1L);

    assertThat(result.getCacheAgeSec()).isEqualTo(360L);
    assertThat(result.getStale()).isTrue();
    assertThat(result.getStaleReason()).isEqualTo("cache_stale");
    assertThat(result.getSafeToEnableOrderActions()).isFalse();
    assertThat(result.getLiveTradingAllowed()).isFalse();
  }

  @Test
  void rejectsRecommendationDetailBeyondDisplayWindow() {
    ReflectionTestUtils.setField(service, "cacheDisplayMaxAgeSeconds", 120L);
    Recommendation recommendation =
        Recommendation.builder()
            .id(1L)
            .ranking(1)
            .tickerCode("005930")
            .companyName("삼성전자")
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .build();
    when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

    assertThatThrownBy(() -> service.findRecommendationDetail(1L))
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
  }

  @Test
  void rejectsRecommendationDetailByStockCodeWhenStaleDisplayIsDisabled() {
    ReflectionTestUtils.setField(service, "allowStaleDisplay", false);
    Recommendation recommendation =
        Recommendation.builder()
            .id(1L)
            .ranking(1)
            .tickerCode("005930")
            .companyName("삼성전자")
            .modelGeneratedAt(generatedAt("2026-06-01T09:55:00+09:00"))
            .build();
    when(recommendationRepository.findByTickerCodeIgnoreCase("005930"))
        .thenReturn(Optional.of(recommendation));

    assertThatThrownBy(() -> service.findRecommendationDetail("005930"))
        .isInstanceOf(GeneralException.class)
        .extracting("code")
        .isEqualTo(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
  }

  @Test
  void marksRecommendationDetailAsStaleWhenGeneratedAtIsMissing() {
    Recommendation recommendation =
        Recommendation.builder().id(1L).ranking(1).tickerCode("005930").companyName("삼성전자").build();
    when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

    RecommendationResDTO.RecommendationDetailDTO result = service.findRecommendationDetail(1L);

    assertThat(result.getCacheAgeSec()).isNull();
    assertThat(result.getStale()).isTrue();
    assertThat(result.getStaleReason()).isEqualTo("recommendation_generated_at_missing");
    assertThat(result.getSafeToEnableOrderActions()).isFalse();
    assertThat(result.getLiveTradingAllowed()).isFalse();
  }
}
