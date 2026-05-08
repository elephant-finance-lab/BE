package com.example.elephantfinancelab_be.domain.watchlist.service.command;

import com.example.elephantfinancelab_be.domain.watchlist.dto.req.WatchlistReqDTO;

public interface WatchlistCommandService {

  void saveGroup(Long userId, WatchlistReqDTO.CreateGroup request);

  void updateGroup(Long userId, Long groupId, WatchlistReqDTO.UpdateGroup request);

  void deleteGroup(Long userId, Long groupId);
}
