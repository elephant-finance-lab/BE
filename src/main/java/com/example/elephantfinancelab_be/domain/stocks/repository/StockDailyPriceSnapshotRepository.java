package com.example.elephantfinancelab_be.domain.stocks.repository;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockDailyPriceSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockDailyPriceSnapshotRepository
    extends JpaRepository<StockDailyPriceSnapshot, Long> {

  Optional<StockDailyPriceSnapshot> findByStock_Ticker(String ticker);
}
