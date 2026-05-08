package com.example.elephantfinancelab_be.domain.watchlist.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

public class WatchlistReqDTO {

  @Getter
  public static class CreateGroup {
    @NotBlank private String name;
  }

  @Getter
  public static class UpdateGroup {
    @NotBlank private String name;
  }
}
