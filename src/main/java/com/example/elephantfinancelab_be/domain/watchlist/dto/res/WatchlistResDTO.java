package com.example.elephantfinancelab_be.domain.watchlist.dto.res;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class WatchlistResDTO {

  @Getter
  @Builder
  @AllArgsConstructor
  public static class GroupDTO {
    private Long groupId;
    private String name;
    private List<ItemDTO> items;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class ItemDTO {
    private Long itemId;
    private String ticker;
  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class GroupListDTO {
    private List<GroupDTO> groups;
  }
}
