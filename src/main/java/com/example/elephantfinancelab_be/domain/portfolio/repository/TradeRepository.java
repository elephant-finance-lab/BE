package com.example.elephantfinancelab_be.domain.portfolio.repository;

import com.example.elephantfinancelab_be.domain.portfolio.entity.Trade;
import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

  Page<Trade> findAllByUserIdAndType(Long userId, TradeType type, Pageable pageable);

  Page<Trade> findAllByUserId(Long userId, Pageable pageable);
}
