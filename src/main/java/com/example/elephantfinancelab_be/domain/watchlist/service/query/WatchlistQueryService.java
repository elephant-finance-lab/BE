package com.example.elephantfinancelab_be.domain.watchlist.service.query;

import com.example.elephantfinancelab_be.domain.watchlist.dto.res.WatchlistResDTO;

public interface WatchlistQueryService {

  WatchlistResDTO.GroupListDTO findGroupList(Long userId);
}
