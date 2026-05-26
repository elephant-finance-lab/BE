package com.example.elephantfinancelab_be.domain.recommendation.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.recommendation.dto.req.RecommendationReqDTO;
import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.recommendation.repository.UserSelectedRecommendationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationCommandServiceImplTest {

  private final RecommendationRepository recommendationRepository =
      mock(RecommendationRepository.class);
  private final UserSelectedRecommendationRepository selectedRepository =
      mock(UserSelectedRecommendationRepository.class);
  private final RecommendationCommandServiceImpl service =
      new RecommendationCommandServiceImpl(recommendationRepository, selectedRepository);

  @Test
  void savesSelectionUsingRecommendationPrimaryKeyFromRequest() {
    Recommendation recommendation =
        Recommendation.builder().id(42L).ranking(1).tickerCode("005930").build();
    when(recommendationRepository.findById(42L)).thenReturn(Optional.of(recommendation));
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(42L)))
        .thenReturn(List.of());

    RecommendationResDTO.RecommendationSelectDTO result =
        service.saveSelectedRecommendations(1L, request(42L));

    assertThat(result.getRecommendationIds()).containsExactly(42L);
    assertThat(result.getStockCodes()).containsExactly("005930");
    assertThat(result.getSelectedCount()).isEqualTo(1);
    verify(recommendationRepository).findById(42L);
    verify(selectedRepository).saveAll(any());
  }

  @Test
  void savesSelectionWithoutPurchaseOptionUsingStockCode() {
    Recommendation recommendation =
        Recommendation.builder().id(43L).ranking(1).tickerCode("000660").build();
    when(recommendationRepository.findByTickerCodeIgnoreCase("000660"))
        .thenReturn(Optional.of(recommendation));
    when(selectedRepository.findAllByUserIdAndRecommendation_IdIn(1L, List.of(43L)))
        .thenReturn(List.of());

    RecommendationResDTO.RecommendationSelectDTO result =
        service.saveSelectedRecommendations(1L, stockCodeRequest("000660"));

    assertThat(result.getRecommendationIds()).containsExactly(43L);
    assertThat(result.getStockCodes()).containsExactly("000660");
    assertThat(result.getSelectedCount()).isEqualTo(1);
    verify(selectedRepository).saveAll(any());
  }

  private static RecommendationReqDTO.SelectRecommendationDTO request(Long recommendationId) {
    RecommendationReqDTO.RecommendationIdDTO item = new RecommendationReqDTO.RecommendationIdDTO();
    ReflectionTestUtils.setField(item, "recommendationId", recommendationId);
    RecommendationReqDTO.SelectRecommendationDTO request =
        new RecommendationReqDTO.SelectRecommendationDTO();
    ReflectionTestUtils.setField(request, "selectedRecommendations", List.of(item));
    return request;
  }

  private static RecommendationReqDTO.SelectRecommendationDTO stockCodeRequest(String stockCode) {
    RecommendationReqDTO.SelectRecommendationDTO request =
        new RecommendationReqDTO.SelectRecommendationDTO();
    ReflectionTestUtils.setField(request, "stockCode", stockCode);
    return request;
  }
}
