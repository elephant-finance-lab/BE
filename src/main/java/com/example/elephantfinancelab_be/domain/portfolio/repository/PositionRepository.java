package com.example.elephantfinancelab_be.domain.portfolio.repository;

import com.example.elephantfinancelab_be.domain.portfolio.entity.Position;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

  List<Position> findAllByUserId(Long userId);

  Page<Position> findAllByUserId(Long userId, Pageable pageable);

  Optional<Position> findByUserIdAndTickerCode(Long userId, String tickerCode);

  boolean existsByUserId(Long userId);
}
