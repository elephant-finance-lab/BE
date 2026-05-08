package com.example.elephantfinancelab_be.domain.watchlist.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class WatchlistReqDTO {

  @Getter
  public static class CreateGroup {
    @NotBlank
    @Size(max = 50)
    private String name;
  }

  @Getter
  public static class UpdateGroup {
    @NotBlank
    @Size(max = 50)
    private String name;
  }

  @Getter
  public static class AddItem {
    @NotNull private Long groupId;
    @NotBlank private String ticker;
  }

  @Getter
  public static class RemoveItem {
    @NotNull private Long groupId;
    @NotBlank private String ticker;
  }
}
