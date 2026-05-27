package com.example.elephantfinancelab_be.domain.stocks.repository;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockInfoPriceSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockInfoPriceSnapshotRepository
    extends JpaRepository<StockInfoPriceSnapshot, Long> {

  Optional<StockInfoPriceSnapshot> findByStock_Ticker(String ticker);
}
