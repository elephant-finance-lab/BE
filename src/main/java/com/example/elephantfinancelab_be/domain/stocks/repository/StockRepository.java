package com.example.elephantfinancelab_be.domain.stocks.repository;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

  Optional<Stock> findByTicker(String ticker);
}
