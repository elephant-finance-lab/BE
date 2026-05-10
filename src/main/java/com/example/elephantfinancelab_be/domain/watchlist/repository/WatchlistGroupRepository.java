package com.example.elephantfinancelab_be.domain.watchlist.repository;

import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {

  List<WatchlistGroup> findByUser_Id(Long userId);
}
