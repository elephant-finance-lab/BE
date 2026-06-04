package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockResolverService {

  private final StockRepository stockRepository;
  private final KisStockBasicInfoClient kisStockBasicInfoClient;
  private final StockRegistrationService stockRegistrationService;
  private final Map<String, Object> resolutionLocks = new ConcurrentHashMap<>();

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public Stock resolve(String ticker) {
    String normalizedTicker = normalizeTicker(ticker);
    return stockRepository
        .findByTicker(normalizedTicker)
        .orElseGet(() -> resolveMissingStock(normalizedTicker));
  }

  private Stock resolveMissingStock(String ticker) {
    Object lock = resolutionLocks.computeIfAbsent(ticker, ignored -> new Object());
    synchronized (lock) {
      try {
        return stockRepository.findByTicker(ticker).orElseGet(() -> fetchAndRegister(ticker));
      } finally {
        resolutionLocks.remove(ticker, lock);
      }
    }
  }

  private Stock fetchAndRegister(String ticker) {
    String stockName = fetchStockNameOrTicker(ticker);
    try {
      Stock stock = stockRegistrationService.saveIfAbsent(ticker, stockName);
      log.info("외부 종목 기본정보 DB 등록 완료. ticker={}, name={}", ticker, stock.getName());
      return stock;
    } catch (DataIntegrityViolationException e) {
      return stockRepository.findByTicker(ticker).orElseThrow(() -> e);
    }
  }

  private String fetchStockNameOrTicker(String ticker) {
    try {
      return kisStockBasicInfoClient.fetchStockName(ticker);
    } catch (StockException e) {
      if (e.getCode() != StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED) {
        throw e;
      }
      log.warn(
          "종목 기본정보 조회 실패로 ticker fallback을 사용합니다. ticker={}, reason={}",
          ticker,
          e.getCode().getCode());
      return ticker;
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.INVALID_STOCK_TICKER);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
