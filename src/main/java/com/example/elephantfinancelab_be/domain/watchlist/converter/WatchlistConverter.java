package com.example.elephantfinancelab_be.domain.watchlist.converter;

import com.example.elephantfinancelab_be.domain.watchlist.dto.res.WatchlistResDTO;
import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistGroup;
import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistItem;
import java.util.List;

public class WatchlistConverter {

  public static WatchlistResDTO.ItemDTO toItemDTO(WatchlistItem item) {
    return WatchlistResDTO.ItemDTO.builder().itemId(item.getId()).ticker(item.getTicker()).build();
  }

  public static WatchlistResDTO.GroupDTO toGroupDTO(WatchlistGroup group) {
    List<WatchlistResDTO.ItemDTO> items =
        group.getItems().stream().map(WatchlistConverter::toItemDTO).toList();
    return WatchlistResDTO.GroupDTO.builder()
        .groupId(group.getId())
        .name(group.getName())
        .items(items)
        .build();
  }

  public static WatchlistResDTO.GroupListDTO toGroupListDTO(List<WatchlistGroup> groups) {
    return WatchlistResDTO.GroupListDTO.builder()
        .groups(groups.stream().map(WatchlistConverter::toGroupDTO).toList())
        .build();
  }
}
