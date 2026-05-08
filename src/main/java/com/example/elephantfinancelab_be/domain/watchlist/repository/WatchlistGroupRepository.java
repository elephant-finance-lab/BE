package com.example.elephantfinancelab_be.domain.watchlist.repository;

import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistGroup;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {
  @EntityGraph(attributePaths = {"items"})
  List<WatchlistGroup> findByUser_Id(Long userId);
}
