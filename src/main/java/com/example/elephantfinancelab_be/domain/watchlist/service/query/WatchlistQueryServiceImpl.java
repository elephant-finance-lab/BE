package com.example.elephantfinancelab_be.domain.watchlist.service.query;

import com.example.elephantfinancelab_be.domain.watchlist.converter.WatchlistConverter;
import com.example.elephantfinancelab_be.domain.watchlist.dto.res.WatchlistResDTO;
import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistGroup;
import com.example.elephantfinancelab_be.domain.watchlist.repository.WatchlistGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchlistQueryServiceImpl implements WatchlistQueryService {

  private final WatchlistGroupRepository watchlistGroupRepository;

  @Override
  public WatchlistResDTO.GroupListDTO findGroupList(Long userId) {
    List<WatchlistGroup> groups = watchlistGroupRepository.findByUser_Id(userId);
    return WatchlistConverter.toGroupListDTO(groups);
  }
}
