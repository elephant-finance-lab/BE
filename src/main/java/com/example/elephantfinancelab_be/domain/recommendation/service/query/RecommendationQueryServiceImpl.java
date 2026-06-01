package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import com.elephant.ai.v1.GetRecommendationsResponse;
import com.elephant.ai.v1.RecommendationItem;
import com.example.elephantfinancelab_be.domain.recommendation.converter.RecommendationConverter;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.exception.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationQueryServiceImpl implements RecommendationQueryService {

  private static final String ANONYMOUS_USER = "anonymousUser";

  private static final DateTimeFormatter MODEL_GENERATED_AT_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
          .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
          .appendOffsetId()
          .toFormatter();

  private final RecommendationRepository recommendationRepository;
  private final UserSelectedRecommendationRepository userSelectedRecommendationRepository;
  private final UserRepository userRepository;
  private final AiServerClient aiServerClient;

  @Value("${ai.recommendations.bundle-id:}")
  private String recommendationBundleId;

  @Value("${ai.recommendations.top-k:10}")
  private int recommendationTopK;

  @Value("${ai.recommendations.include-diagnostics:false}")
  private boolean includeRecommendationDiagnostics;

  @Value("${ai.recommendations.cache-read-enabled:false}")
  private boolean cacheReadEnabled;

  @Value("${ai.recommendations.cache.max-age-seconds:180}")
  private long cacheMaxAgeSeconds;

  @Value("${ai.recommendations.cache.serve-stale:true}")
  private boolean serveStaleCache;

  @Value("${ai.recommendations.cache.max-stale-age-seconds:86400}")
  private long cacheMaxStaleAgeSeconds;

  @Value("${ai.recommendations.cache.max-future-skew-seconds:5}")
  private long cacheMaxFutureSkewSeconds;

  private Clock clock = Clock.systemUTC();

  @Override
  @Transactional
  public RecommendationResDTO.RecommendationListDTO findRecommendationList(String email) {
    if (cacheReadEnabled) {
      try {
        return findCachedRecommendationList(email);
      } catch (GeneralException e) {
        log.info(
            "[Recommendation] cached recommendations unavailable. refreshing from AI. code={}",
            e.getCode().getCode());
      }
    }
    return refreshModelRecommendations(email);
  }

  @Override
  @Transactional
  public RecommendationResDTO.RecommendationListDTO refreshModelRecommendations() {
    return refreshModelRecommendations(null);
  }

  private RecommendationResDTO.RecommendationListDTO refreshModelRecommendations(String email) {
    GetRecommendationsResponse response =
        aiServerClient.getRecommendations(
            recommendationBundleId, recommendationTopK, includeRecommendationDiagnostics);
    if (!"PASS".equalsIgnoreCase(response.getStatus())) {
      log.warn(
          "[Recommendation] AI model response unavailable: status={}, reason={}",
          response.getStatus(),
          response.getReason());
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }

    OffsetDateTime modelGeneratedAt = parseGeneratedAt(response.getGeneratedAt());
    List<Recommendation> recommendations =
        deduplicateByStockCode(response).values().stream()
            .map(item -> upsertModelRecommendation(item, response, modelGeneratedAt))
            .toList();
    List<Recommendation> savedRecommendations = recommendationRepository.saveAll(recommendations);
    Set<Long> selectedRecommendationIds =
        findSelectedRecommendationIds(email, savedRecommendations);
    List<RecommendationResDTO.RecommendationInfoDTO> infoList =
        savedRecommendations.stream()
            .map(
                recommendation ->
                    RecommendationConverter.toRecommendationInfoDTO(
                        recommendation,
                        recommendation.getId() != null
                            && selectedRecommendationIds.contains(recommendation.getId())))
            .toList();
    return RecommendationConverter.toRecommendationListDTO(
        "사용자 맞춤 투자 추천 리스트",
        response.getStatus(),
        response.getReason(),
        formatGeneratedAt(modelGeneratedAt),
        response.getBundleId(),
        response.getModelVersion(),
        response.getAsof(),
        response.getMode(),
        infoList);
  }

  private RecommendationResDTO.RecommendationListDTO findCachedRecommendationList(String email) {
    Recommendation latest = findLatestCachedRecommendation();
    OffsetDateTime latestGeneratedAt = latest.getModelGeneratedAt();
    long cacheAgeSec = cacheAgeSec(latestGeneratedAt);
    boolean stale = cacheAgeSec > cacheMaxAgeSeconds;
    if (stale && !canServeStaleCache(cacheAgeSec)) {
      log.warn(
          "[Recommendation] cached model recommendations stale: generatedAt={}, ageSec={}, maxAgeSec={}",
          formatGeneratedAt(latestGeneratedAt),
          cacheAgeSec,
          cacheMaxAgeSeconds);
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
    List<Recommendation> savedRecommendations =
        recommendationRepository.findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
            latestGeneratedAt, latest.getModelBundleId());
    if (savedRecommendations.isEmpty()) {
      log.warn("[Recommendation] cached model recommendations unavailable");
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
    if (stale) {
      log.warn(
          "[Recommendation] returning stale cached model recommendations: generatedAt={}, ageSec={}, maxAgeSec={}, maxStaleAgeSec={}",
          formatGeneratedAt(latestGeneratedAt),
          cacheAgeSec,
          cacheMaxAgeSeconds,
          cacheMaxStaleAgeSeconds);
    }
    List<RecommendationResDTO.RecommendationInfoDTO> infoList =
        toRecommendationInfoDTOs(email, savedRecommendations);
    return RecommendationConverter.toRecommendationListDTO(
        "사용자 맞춤 투자 추천 리스트",
        "PASS",
        stale ? "cached_recommendations_stale" : "cached_recommendations",
        formatGeneratedAt(latestGeneratedAt),
        latest.getModelBundleId(),
        latest.getModelVersion(),
        latest.getModelAsof(),
        "cached",
        cacheAgeSec,
        stale,
        stale ? "cache_age_exceeded" : null,
        infoList);
  }

  @Override
  public RecommendationResDTO.RecommendationDetailDTO findRecommendationDetail(
      Long recommendationId) {
    Recommendation recommendation =
        recommendationRepository
            .findById(recommendationId)
            .orElseThrow(
                () -> new GeneralException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
    return RecommendationConverter.toRecommendationDetailDTO(recommendation, "맞춤형 투자 전략 분석");
  }

  @Override
  public RecommendationResDTO.RecommendationDetailDTO findRecommendationDetail(String stockCode) {
    Recommendation recommendation =
        recommendationRepository
            .findByTickerCodeIgnoreCase(stockCode.trim())
            .orElseThrow(
                () -> new GeneralException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
    return RecommendationConverter.toRecommendationDetailDTO(recommendation, "맞춤형 투자 전략 분석");
  }

  @Override
  public Long findUserIdByEmail(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }

  private List<RecommendationResDTO.RecommendationInfoDTO> toRecommendationInfoDTOs(
      String email, List<Recommendation> recommendations) {
    Set<Long> selectedRecommendationIds = findSelectedRecommendationIds(email, recommendations);
    return recommendations.stream()
        .map(
            recommendation ->
                RecommendationConverter.toRecommendationInfoDTO(
                    recommendation,
                    recommendation.getId() != null
                        && selectedRecommendationIds.contains(recommendation.getId())))
        .toList();
  }

  private Set<Long> findSelectedRecommendationIds(
      String email, List<Recommendation> recommendations) {
    if (isUnauthenticated(email) || recommendations.isEmpty()) {
      return Set.of();
    }
    List<Long> recommendationIds =
        recommendations.stream().map(Recommendation::getId).filter(id -> id != null).toList();
    if (recommendationIds.isEmpty()) {
      return Set.of();
    }
    Long userId = findUserIdByEmail(email);
    return userSelectedRecommendationRepository
        .findAllByUserIdAndRecommendation_IdIn(userId, recommendationIds)
        .stream()
        .map(item -> item.getRecommendation().getId())
        .collect(Collectors.toSet());
  }

  private static boolean isUnauthenticated(String email) {
    return email == null || email.isBlank() || ANONYMOUS_USER.equals(email);
  }

  private Map<String, RecommendationItem> deduplicateByStockCode(
      GetRecommendationsResponse response) {
    Map<String, RecommendationItem> deduplicated = new LinkedHashMap<>();
    response.getRecommendationsList().stream()
        .sorted(Comparator.comparingInt(RecommendationItem::getRanking))
        .forEach(
            item -> {
              String stockCode = normalizedStockCode(item);
              deduplicated.putIfAbsent(stockCode, item);
            });
    return deduplicated;
  }

  private Recommendation upsertModelRecommendation(
      RecommendationItem item,
      GetRecommendationsResponse response,
      OffsetDateTime modelGeneratedAt) {
    String stockCode = normalizedStockCode(item);
    Recommendation recommendation =
        recommendationRepository
            .findByTickerCodeIgnoreCase(stockCode)
            .orElseGet(() -> Recommendation.builder().tickerCode(stockCode).build());
    recommendation.updateModelResult(
        item.getRecommendationId(),
        item.getStockName(),
        item.getRanking(),
        item.getScore(),
        item.getReason(),
        item.getExpectedReturnAvailable() ? item.getExpectedReturn() : null,
        item.getExpectedReturnAvailable(),
        item.getRiskLevel(),
        item.getModelVersion().isBlank() ? response.getModelVersion() : item.getModelVersion(),
        item.getBundleId().isBlank() ? response.getBundleId() : item.getBundleId(),
        modelGeneratedAt,
        response.getAsof());
    return recommendation;
  }

  private String normalizedStockCode(RecommendationItem item) {
    String stockCode =
        item.getStockCode().isBlank() ? item.getTicker().trim() : item.getStockCode().trim();
    if (stockCode.isBlank()) {
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
    return stockCode.toUpperCase(Locale.ROOT);
  }

  private long cacheAgeSec(OffsetDateTime generated) {
    if (generated == null) {
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
    OffsetDateTime now = OffsetDateTime.now(clock);
    if (generated.toInstant().isAfter(now.plusSeconds(cacheMaxFutureSkewSeconds).toInstant())) {
      log.warn(
          "[Recommendation] cached model recommendations have future generatedAt={}, maxFutureSkewSec={}",
          formatGeneratedAt(generated),
          cacheMaxFutureSkewSeconds);
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
    long ageSec = Duration.between(generated, now).getSeconds();
    return Math.max(0L, ageSec);
  }

  private boolean canServeStaleCache(long cacheAgeSec) {
    return serveStaleCache && cacheMaxStaleAgeSeconds > 0 && cacheAgeSec <= cacheMaxStaleAgeSeconds;
  }

  private Recommendation findLatestCachedRecommendation() {
    return recommendationRepository.findByModelGeneratedAtIsNotNull().stream()
        .sorted(
            Comparator.comparing(
                    (Recommendation recommendation) ->
                        recommendation.getModelGeneratedAt().toInstant())
                .reversed()
                .thenComparing(
                    Recommendation::getRanking, Comparator.nullsLast(Comparator.naturalOrder())))
        .findFirst()
        .orElseThrow(
            () -> new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE));
  }

  private OffsetDateTime parseGeneratedAt(String generatedAt) {
    try {
      return OffsetDateTime.parse(generatedAt);
    } catch (DateTimeParseException | NullPointerException e) {
      log.warn("[Recommendation] AI model response has invalid generatedAt={}", generatedAt);
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
  }

  private String formatGeneratedAt(OffsetDateTime generatedAt) {
    return generatedAt == null ? null : MODEL_GENERATED_AT_FORMATTER.format(generatedAt);
  }
}
