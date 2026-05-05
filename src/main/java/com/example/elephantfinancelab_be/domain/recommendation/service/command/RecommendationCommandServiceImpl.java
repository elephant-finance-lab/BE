package com.example.elephantfinancelab_be.domain.recommendation.service.command;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.global.apiPayload.code.RecommendationErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.GeneralException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationCommandServiceImpl implements RecommendationCommandService {

  @Override
  public RecommendationResDTO.RecommendationSelectDTO saveSelectedRecommendations(
      RecommendationReqDTO.SelectRecommendationDTO request) {

    if (request == null || request.getSelectedRecommendations() == null) {
      throw new GeneralException(RecommendationErrorCode.NO_SELECTED_RECOMMENDATION);
    }

    List<Long> ids =
        request.getSelectedRecommendations().stream()
            .filter(Objects::nonNull)
            .map(RecommendationReqDTO.RecommendationIdDTO::getRecommendationId)
            .filter(Objects::nonNull)
            .toList();

    if (ids.isEmpty()) {
      throw new GeneralException(RecommendationErrorCode.NO_SELECTED_RECOMMENDATION);
    }

    // TODO: 실제 저장 로직 구현 시 여기에 repository.saveAll() 추가
    return RecommendationResDTO.RecommendationSelectDTO.builder()
        .selectedCount(ids.size())
        .recommendationIds(ids)
        .build();
  }

  @Override
  public RecommendationResDTO.PurchaseOptionDTO savePurchaseOption(
      RecommendationReqDTO.PurchaseOptionRequestDTO request) {

    // TODO: 실제 선행 선택 여부 검증 로직 추가
    // TODO: DB 연동 시 optionId로 실제 데이터 조회

    return switch (request.getOptionId()) {
      case 1 ->
          RecommendationResDTO.PurchaseOptionDTO.builder()
              .optionId(1)
              .minRate(5)
              .maxRate(10)
              .label("5 ~ 10%")
              .build();
      case 2 ->
          RecommendationResDTO.PurchaseOptionDTO.builder()
              .optionId(2)
              .minRate(10)
              .maxRate(15)
              .label("10 ~ 15%")
              .build();
      case 3 ->
          RecommendationResDTO.PurchaseOptionDTO.builder()
              .optionId(3)
              .minRate(15)
              .maxRate(20)
              .label("15 ~ 20%")
              .build();
      case 4 ->
          RecommendationResDTO.PurchaseOptionDTO.builder()
              .optionId(4)
              .minRate(20)
              .maxRate(30)
              .label("20 ~ 30%")
              .build();
      default -> throw new GeneralException(RecommendationErrorCode.NO_PRIOR_SELECTION);
    };
  }
}
