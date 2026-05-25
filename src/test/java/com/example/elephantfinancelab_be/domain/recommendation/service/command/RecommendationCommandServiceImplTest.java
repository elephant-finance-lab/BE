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
    when(recommendationRepository.findAllById(List.of(42L))).thenReturn(List.of(recommendation));

    RecommendationResDTO.RecommendationSelectDTO result =
        service.saveSelectedRecommendations(1L, request(42L));

    assertThat(result.getRecommendationIds()).containsExactly(42L);
    assertThat(result.getSelectedCount()).isEqualTo(1);
    verify(recommendationRepository).findAllById(List.of(42L));
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
}
