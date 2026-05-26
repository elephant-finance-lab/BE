package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockDailyPriceResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockChartClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockDailyPriceRedisService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSnapshotPersistenceService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockDailyPriceQueryServiceImpl implements StockDailyPriceQueryService {

  private final StockResolverService stockResolverService;
  private final StockDailyPriceRedisService stockDailyPriceRedisService;
  private final StockSnapshotPersistenceService stockSnapshotPersistenceService;
  private final KisStockChartClient kisStockChartClient;

  @Override
  public StockDailyPriceResDTO.DailyPrices getDailyPrices(String ticker) {
    String normalizedTicker = normalizeTicker(ticker);
    StockDailyPriceResDTO.DailyPrices cachedDailyPrices = findCachedDailyPrices(normalizedTicker);
    if (cachedDailyPrices != null) {
      log.info("종목 일별 시세 캐시 조회 성공. ticker={}", normalizedTicker);
      return cachedDailyPrices;
    }

    Stock stock = stockResolverService.resolve(normalizedTicker);
    StockDailyPriceResDTO.DailyPrices storedDailyPrices = findStoredDailyPrices(stock.getTicker());
    if (storedDailyPrices != null) {
      saveDailyPriceCache(storedDailyPrices);
      log.info("종목 일별 시세 DB snapshot 조회 성공. ticker={}", normalizedTicker);
      return storedDailyPrices;
    }

    List<StockDailyPriceResDTO.Item> items =
        kisStockChartClient.fetchDailyPrices(stock.getTicker());
    StockDailyPriceResDTO.DailyPrices dailyPrices =
        new StockDailyPriceResDTO.DailyPrices(stock.getTicker(), items);
    saveDailyPriceSnapshot(stock, dailyPrices);
    saveDailyPriceCache(dailyPrices);
    return dailyPrices;
  }

  private StockDailyPriceResDTO.DailyPrices findStoredDailyPrices(String ticker) {
    try {
      return stockSnapshotPersistenceService.findDailyPrices(ticker);
    } catch (RuntimeException e) {
      log.warn("종목 일별 시세 DB snapshot 조회 실패로 KIS 조회를 계속합니다. ticker={}", ticker, e);
      return null;
    }
  }

  private void saveDailyPriceSnapshot(Stock stock, StockDailyPriceResDTO.DailyPrices dailyPrices) {
    try {
      stockSnapshotPersistenceService.saveDailyPrices(stock, dailyPrices);
    } catch (RuntimeException e) {
      log.warn("종목 일별 시세 DB snapshot 저장 실패를 무시합니다. ticker={}", stock.getTicker(), e);
    }
  }

  private StockDailyPriceResDTO.DailyPrices findCachedDailyPrices(String ticker) {
    try {
      return stockDailyPriceRedisService.find(ticker);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_DAILY_PRICE_CACHE_DESERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_DAILY_PRICE_CACHE_DESERIALIZE_FAILED.getMessage(),
          ticker,
          e);
      return null;
    }
  }

  private void saveDailyPriceCache(StockDailyPriceResDTO.DailyPrices dailyPrices) {
    try {
      stockDailyPriceRedisService.save(dailyPrices);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_DAILY_PRICE_CACHE_SERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_DAILY_PRICE_CACHE_SERIALIZE_FAILED.getMessage(),
          dailyPrices.ticker(),
          e);
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.INVALID_STOCK_TICKER);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
