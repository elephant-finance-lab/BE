package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import java.util.Locale;
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateNameIfTickerEcho(String ticker, String stockName) {
    String normalizedTicker = normalizeTicker(ticker);
    String normalizedName = stockName == null ? null : stockName.trim();
    if (normalizedName == null
        || normalizedName.isBlank()
        || normalizedName.equalsIgnoreCase(normalizedTicker)) {
      return;
    }

    stockRepository
        .findByTicker(normalizedTicker)
        .filter(stock -> stock.getName().trim().equalsIgnoreCase(normalizedTicker))
        .ifPresent(
            stock -> {
              stock.updateName(normalizedName);
              stockRepository.saveAndFlush(stock);
            });
  }

  private String normalizeTicker(String ticker) {
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
