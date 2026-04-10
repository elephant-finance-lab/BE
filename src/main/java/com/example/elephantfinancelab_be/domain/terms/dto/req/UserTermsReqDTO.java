package com.example.elephantfinancelab_be.domain.terms.dto.req;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserTermsReqDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AgreeAll {

    @NotNull(message = "전체 동의 여부는 필수입니다.")
    @AssertTrue(message = "네 가지 약관에 모두 동의해야 합니다.")
    private Boolean agreeAll;
  }
}
