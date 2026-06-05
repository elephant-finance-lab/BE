package com.example.elephantfinancelab_be.domain.autotrading.dto.req;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AutoTradingReqDTOTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void startSessionAllowsNullAndZeroAiDefaultFields() {
    assertThat(validator.validate(request(null, null))).isEmpty();
    assertThat(validator.validate(request(0, 0))).isEmpty();
  }

  @Test
  void startSessionRejectsNegativeAiDefaultFields() {
    assertThat(validator.validate(request(-1, -1)))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("cycles", "intervalSec");
  }

  @Test
  void startSessionAllowsEmptyRecommendationIdsForLatestRecommendations() {
    AutoTradingReqDTO.StartSession request = request(3, 60);
    ReflectionTestUtils.setField(request, "recommendationIds", java.util.List.of());

    assertThat(validator.validate(request)).isEmpty();
  }

  private static AutoTradingReqDTO.StartSession request(Integer cycles, Integer intervalSec) {
    AutoTradingReqDTO.StartSession request = new AutoTradingReqDTO.StartSession();
    ReflectionTestUtils.setField(request, "recommendationIds", java.util.List.of(1L));
    ReflectionTestUtils.setField(request, "purchaseOptionId", 2);
    ReflectionTestUtils.setField(request, "cycles", cycles);
    ReflectionTestUtils.setField(request, "intervalSec", intervalSec);
    return request;
  }
}
