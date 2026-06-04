package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockRegistrationService {

  private final StockRepository stockRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Stock saveIfAbsent(String ticker, String stockName) {
    return stockRepository
        .findByTicker(ticker)
        .orElseGet(
            () ->
                stockRepository.saveAndFlush(
                    Stock.builder().ticker(ticker).name(stockName).build()));
  }
}
