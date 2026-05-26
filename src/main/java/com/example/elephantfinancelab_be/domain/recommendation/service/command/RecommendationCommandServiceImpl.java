package com.example.elephantfinancelab_be.domain.recommendation.service.command;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import com.example.elephantfinancelab_be.domain.recommendation.exception.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationCommandServiceImpl implements RecommendationCommandService {

  private final RecommendationRepository recommendationRepository;
  private final UserSelectedRecommendationRepository userSelectedRecommendationRepository;

  @Override
  public RecommendationResDTO.RecommendationSelectDTO saveSelectedRecommendations(
      Long userId, RecommendationReqDTO.SelectRecommendationDTO request) {

    List<RecommendationSelection> selections = extractSelections(request);
    Map<Long, Recommendation> recommendationById = new LinkedHashMap<>();
    for (RecommendationSelection selection : selections) {
      Recommendation recommendation = findRecommendation(selection);
      recommendationById.putIfAbsent(recommendation.getId(), recommendation);
    }

    List<Recommendation> recommendations = List.copyOf(recommendationById.values());
    List<Long> ids = recommendations.stream().map(Recommendation::getId).toList();
    Set<Long> alreadySelectedIds =
        userSelectedRecommendationRepository
            .findAllByUserIdAndRecommendation_IdIn(userId, ids)
            .stream()
            .map(item -> item.getRecommendation().getId())
            .collect(Collectors.toSet());

    List<UserSelectedRecommendation> entitiesToSave =
        recommendations.stream()
            .filter(recommendation -> !alreadySelectedIds.contains(recommendation.getId()))
            .map(
                recommendation ->
                    UserSelectedRecommendation.builder()
                        .userId(userId)
                        .recommendation(recommendation)
                        .build())
            .toList();

    if (!entitiesToSave.isEmpty()) {
      userSelectedRecommendationRepository.saveAll(entitiesToSave);
    }

    return RecommendationResDTO.RecommendationSelectDTO.builder()
        .selectedCount(ids.size())
        .recommendationIds(ids)
        .stockCodes(recommendations.stream().map(Recommendation::getTickerCode).toList())
        .build();
  }

  private List<RecommendationSelection> extractSelections(
      RecommendationReqDTO.SelectRecommendationDTO request) {
    if (request == null) {
      throw new GeneralException(RecommendationErrorCode.NO_SELECTED_RECOMMENDATION);
    }

    List<RecommendationSelection> selections = new ArrayList<>();
    if (request.getRecommendationId() != null || hasText(request.getStockCode())) {
      selections.add(
          new RecommendationSelection(request.getRecommendationId(), request.getStockCode()));
    }
    if (request.getSelectedRecommendations() != null) {
      request
          .getSelectedRecommendations()
          .forEach(
              item -> {
                if (item != null
                    && (item.getRecommendationId() != null || hasText(item.getStockCode()))) {
                  selections.add(
                      new RecommendationSelection(item.getRecommendationId(), item.getStockCode()));
                }
              });
    }
    if (selections.isEmpty()) {
      throw new GeneralException(RecommendationErrorCode.NO_SELECTED_RECOMMENDATION);
    }
    return selections;
  }

  private Recommendation findRecommendation(RecommendationSelection selection) {
    if (selection.recommendationId() != null) {
      Recommendation recommendation =
          recommendationRepository
              .findById(selection.recommendationId())
              .orElseThrow(
                  () -> new GeneralException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
      if (hasText(selection.stockCode())
          && !recommendation.getTickerCode().equalsIgnoreCase(selection.stockCode().trim())) {
        throw new GeneralException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND);
      }
      return recommendation;
    }
    return recommendationRepository
        .findByTickerCodeIgnoreCase(selection.stockCode().trim())
        .orElseThrow(() -> new GeneralException(RecommendationErrorCode.RECOMMENDATION_NOT_FOUND));
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record RecommendationSelection(Long recommendationId, String stockCode) {}
}
