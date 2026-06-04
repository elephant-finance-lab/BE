package com.example.elephantfinancelab_be.domain.stocks.repository;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockChartSnapshotRepository extends JpaRepository<StockChartSnapshot, Long> {

  Optional<StockChartSnapshot> findByStock_TickerAndChartRange(
      String ticker, StockChartRange chartRange);
}
