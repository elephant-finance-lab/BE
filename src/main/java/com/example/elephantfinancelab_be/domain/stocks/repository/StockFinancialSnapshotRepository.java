package com.example.elephantfinancelab_be.domain.stocks.repository;

import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialSnapshot;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockFinancialSnapshotRepository
    extends JpaRepository<StockFinancialSnapshot, Long> {

  Optional<StockFinancialSnapshot> findByStock_TickerAndStatementAndPeriod(
      String ticker, StockFinancialStatement statement, StockFinancialPeriod period);
}
