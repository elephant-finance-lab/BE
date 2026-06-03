package com.example.elephantfinancelab_be.domain.autotrading.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AutoTradingReqDTO {

  @Getter
  @NoArgsConstructor
  public static class StartSession {
    @NotEmpty private List<@NotNull Long> recommendationIds;

    private String bundleId;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer purchaseOptionId;

    @NotNull
    @Min(1)
    private Integer cycles;

    @NotNull
    @Min(60)
    private Integer intervalSec;
  }
}
