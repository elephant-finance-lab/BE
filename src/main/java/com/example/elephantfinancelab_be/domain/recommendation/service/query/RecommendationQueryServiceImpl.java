package com.example.elephantfinancelab_be.domain.recommendation.service.query;

import com.elephant.ai.v1.GetRecommendationsResponse;
import com.elephant.ai.v1.RecommendationItem;
import com.example.elephantfinancelab_be.domain.recommendation.converter.RecommendationConverter;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.exception.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import java.util.Comparator;
import java.util.List;
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

  private final RecommendationRepository recommendationRepository;
  private final UserRepository userRepository;
  private final AiServerClient aiServerClient;

  @Value("${ai.recommendations.bundle-id:}")
  private String recommendationBundleId;

  @Value("${ai.recommendations.top-k:10}")
  private int recommendationTopK;

  @Value("${ai.recommendations.include-diagnostics:false}")
  private boolean includeRecommendationDiagnostics;

  @Override
  @Transactional
  public RecommendationResDTO.RecommendationListDTO findRecommendationList() {
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

    List<Recommendation> recommendations =
        response.getRecommendationsList().stream()
            .sorted(Comparator.comparingInt(RecommendationItem::getRanking))
            .map(item -> upsertModelRecommendation(item, response))
            .toList();
    List<Recommendation> savedRecommendations = recommendationRepository.saveAll(recommendations);
    List<RecommendationResDTO.RecommendationInfoDTO> infoList =
        savedRecommendations.stream()
            .map(RecommendationConverter::toRecommendationInfoDTO)
            .toList();
    return RecommendationConverter.toRecommendationListDTO(
        "사용자 맞춤 투자 추천 리스트",
        response.getStatus(),
        response.getReason(),
        response.getGeneratedAt(),
        response.getBundleId(),
        response.getModelVersion(),
        response.getAsof(),
        response.getMode(),
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

  private Recommendation upsertModelRecommendation(
      RecommendationItem item, GetRecommendationsResponse response) {
    String stockCode =
        item.getStockCode().isBlank() ? item.getTicker().trim() : item.getStockCode().trim();
    if (stockCode.isBlank()) {
      throw new GeneralException(RecommendationErrorCode.MODEL_RECOMMENDATION_UNAVAILABLE);
    }
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
        response.getGeneratedAt(),
        response.getAsof());
    return recommendation;
  }
}
