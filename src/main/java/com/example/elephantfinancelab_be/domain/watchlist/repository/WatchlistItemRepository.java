package com.example.elephantfinancelab_be.domain.watchlist.repository;

import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

  List<WatchlistItem> findByGroup_Id(Long groupId);

  boolean existsByGroup_IdAndTicker(Long groupId, String ticker);

  long deleteByGroup_IdAndTicker(Long groupId, String ticker);
}
