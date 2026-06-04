package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceClient;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceWebSocketClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSummaryRedisService;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryServiceImpl implements StockQueryService {

  private static final int SUMMARY_FETCH_ATTEMPTS = 3;
  private static final Duration SUMMARY_FETCH_RETRY_DELAY = Duration.ofMillis(400);

  private final StockResolverService stockResolverService;
  private final StockSummaryRedisService stockSummaryRedisService;
  private final KisStockPriceClient kisStockPriceClient;
  private final KisStockPriceWebSocketClient kisStockPriceWebSocketClient;

  @Override
  public StockResDTO.Summary getSummary(String ticker) {
    String normalizedTicker = normalizeTicker(ticker);
    StockResDTO.Summary cachedSummary = findCachedSummary(normalizedTicker);
    if (cachedSummary != null) {
      kisStockPriceWebSocketClient.subscribe(normalizedTicker);
      log.info("종목 요약 캐시 조회 성공. ticker={}", normalizedTicker);
      return cachedSummary;
    }

    Stock stock = stockResolverService.resolve(normalizedTicker);
    StockResDTO.Summary summary = fetchSummaryWithRetry(stock);
    saveSummaryCache(summary);
    kisStockPriceWebSocketClient.subscribe(stock.getTicker());
    return summary;
  }

  private StockResDTO.Summary fetchSummaryWithRetry(Stock stock) {
    StockException lastException = null;
    for (int attempt = 1; attempt <= SUMMARY_FETCH_ATTEMPTS; attempt++) {
      try {
        return kisStockPriceClient.fetchSummary(stock);
      } catch (StockException e) {
        lastException = e;
        if (!isRetryableKisSummaryError(e) || attempt == SUMMARY_FETCH_ATTEMPTS) {
          throw e;
        }
        log.warn(
            "종목 요약 KIS 조회 재시도. ticker={}, attempt={}/{}, reason={}",
            stock.getTicker(),
            attempt,
            SUMMARY_FETCH_ATTEMPTS,
            e.getCode().getCode());
        sleep(SUMMARY_FETCH_RETRY_DELAY);
      }
    }
    throw lastException == null
        ? new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED)
        : lastException;
  }

  private boolean isRetryableKisSummaryError(StockException e) {
    return e.getCode() == StockErrorCode.KIS_STOCK_PRICE_API_FAILED;
  }

  private void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private StockResDTO.Summary findCachedSummary(String ticker) {
    try {
      return stockSummaryRedisService.find(ticker);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_SUMMARY_CACHE_DESERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_SUMMARY_CACHE_DESERIALIZE_FAILED.getMessage(),
          ticker,
          e);
      return null;
    }
  }

  private void saveSummaryCache(StockResDTO.Summary summary) {
    try {
      stockSummaryRedisService.save(summary);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_SUMMARY_CACHE_SAVE_FAILED.getCode(),
          StockErrorCode.STOCK_SUMMARY_CACHE_SAVE_FAILED.getMessage(),
          summary.getTicker(),
          e);
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.STOCK_NOT_FOUND);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
