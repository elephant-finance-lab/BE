package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockChartClient;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceWebSocketClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockChartRedisService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSnapshotPersistenceService;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockChartQueryServiceImpl implements StockChartQueryService {

  private static final String CURRENCY_KRW = "KRW";
  private static final int ONE_WEEK_MIN_POINT_COUNT = 7;
  private static final Duration ONE_DAY_FETCH_LOCK_TTL = Duration.ofSeconds(20);
  private static final Duration ONE_DAY_FETCH_WAIT_INTERVAL = Duration.ofMillis(300);
  private static final int ONE_DAY_FETCH_WAIT_ATTEMPTS = 50;

  private final StockResolverService stockResolverService;
  private final StockChartRedisService stockChartRedisService;
  private final StockSnapshotPersistenceService stockSnapshotPersistenceService;
  private final KisStockChartClient kisStockChartClient;
  private final KisStockPriceWebSocketClient kisStockPriceWebSocketClient;

  @Override
  public StockChartResDTO.Chart getChart(String ticker, String range, String type) {
    StockChartRange chartRange = StockChartRange.from(range);
    StockChartType chartType = StockChartType.from(type);
    String normalizedTicker = normalizeTicker(ticker);

    StockChartResDTO.Chart cachedChart = findCachedChart(normalizedTicker, chartRange, chartType);
    if (cachedChart != null) {
      if (!isUsableChart(cachedChart, chartRange)) {
        log.info(
            "종목 차트 캐시 데이터가 부족해 KIS 재조회를 계속합니다. ticker={}, range={}, count={}",
            normalizedTicker,
            chartRange.getValue(),
            pointCount(cachedChart));
      } else {
        subscribeRealtimeIfNeeded(normalizedTicker, chartRange);
        log.info("종목 차트 캐시 조회 성공. ticker={}, range={}, type={}", normalizedTicker, range, type);
        return cachedChart;
      }
    }

    Stock stock = stockResolverService.resolve(normalizedTicker);
    StockChartResDTO.Chart storedChart = findStoredChart(stock.getTicker(), chartRange, chartType);
    if (storedChart != null) {
      if (!isUsableChart(storedChart, chartRange)) {
        log.info(
            "종목 차트 DB snapshot 데이터가 부족해 KIS 재조회를 계속합니다. ticker={}, range={}, count={}",
            normalizedTicker,
            chartRange.getValue(),
            pointCount(storedChart));
      } else {
        saveChartCache(storedChart);
        log.info(
            "종목 차트 DB snapshot 조회 성공. ticker={}, range={}, type={}", normalizedTicker, range, type);
        return storedChart;
      }
    }

    String lockToken = null;
    boolean lockAcquired = false;
    if (chartRange == StockChartRange.ONE_DAY) {
      lockToken = UUID.randomUUID().toString();
      lockAcquired = acquireOneDayFetchLock(stock.getTicker(), lockToken);
      if (!lockAcquired) {
        StockChartResDTO.Chart waitedChart =
            waitForCachedChart(stock.getTicker(), chartRange, chartType);
        if (waitedChart != null) {
          subscribeRealtimeIfNeeded(stock.getTicker(), chartRange);
          return waitedChart;
        }
      }
    }

    try {
      List<StockChartResDTO.DataPoint> dataPoints =
          kisStockChartClient.fetchChart(stock.getTicker(), chartRange);
      StockChartResDTO.Chart chart =
          new StockChartResDTO.Chart(
              stock.getTicker(),
              chartRange.getValue(),
              chartType,
              chartRange.getInterval(),
              CURRENCY_KRW,
              dataPoints);
      saveChartSnapshot(stock, chartRange, chart);
      saveChartCache(chart);
      saveEquivalentOneDayChartCache(chart);
      subscribeRealtimeIfNeeded(stock.getTicker(), chartRange);
      return chart;
    } finally {
      if (lockAcquired) {
        releaseOneDayFetchLock(stock.getTicker(), lockToken);
      }
    }
  }

  private boolean acquireOneDayFetchLock(String ticker, String lockToken) {
    try {
      return stockChartRedisService.acquireUpdateLock(
          ticker, StockChartRange.ONE_DAY, StockChartType.LINE, lockToken, ONE_DAY_FETCH_LOCK_TTL);
    } catch (RuntimeException e) {
      log.warn("1일 차트 조회 락 획득 실패를 무시합니다. ticker={}", ticker, e);
      return true;
    }
  }

  private void releaseOneDayFetchLock(String ticker, String lockToken) {
    try {
      stockChartRedisService.releaseUpdateLock(
          ticker, StockChartRange.ONE_DAY, StockChartType.LINE, lockToken);
    } catch (RuntimeException e) {
      log.warn("1일 차트 조회 락 해제 실패를 무시합니다. ticker={}", ticker, e);
    }
  }

  private StockChartResDTO.Chart waitForCachedChart(
      String ticker, StockChartRange chartRange, StockChartType chartType) {
    for (int attempt = 0; attempt < ONE_DAY_FETCH_WAIT_ATTEMPTS; attempt++) {
      sleep(ONE_DAY_FETCH_WAIT_INTERVAL);
      StockChartResDTO.Chart cachedChart = findCachedChart(ticker, chartRange, chartType);
      if (cachedChart != null && isUsableChart(cachedChart, chartRange)) {
        log.info(
            "종목 차트 진행 중 조회가 끝난 캐시를 사용합니다. ticker={}, range={}, type={}",
            ticker,
            chartRange.getValue(),
            chartType);
        return cachedChart;
      }
    }
    return null;
  }

  private void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void saveEquivalentOneDayChartCache(StockChartResDTO.Chart chart) {
    if (!StockChartRange.ONE_DAY.getValue().equals(chart.range())) {
      return;
    }

    StockChartType otherType =
        chart.type() == StockChartType.LINE ? StockChartType.CANDLE : StockChartType.LINE;
    StockChartResDTO.Chart equivalentChart =
        new StockChartResDTO.Chart(
            chart.ticker(),
            chart.range(),
            otherType,
            chart.interval(),
            chart.currency(),
            chart.data());
    saveChartCache(equivalentChart);
  }

  private boolean isUsableChart(StockChartResDTO.Chart chart, StockChartRange chartRange) {
    if (chartRange != StockChartRange.ONE_WEEK) {
      return true;
    }
    return pointCount(chart) >= ONE_WEEK_MIN_POINT_COUNT;
  }

  private int pointCount(StockChartResDTO.Chart chart) {
    return chart.data() == null ? 0 : chart.data().size();
  }

  private StockChartResDTO.Chart findStoredChart(
      String ticker, StockChartRange chartRange, StockChartType chartType) {
    try {
      return stockSnapshotPersistenceService.findChart(ticker, chartRange, chartType);
    } catch (RuntimeException e) {
      log.warn(
          "종목 차트 DB snapshot 조회 실패로 KIS 조회를 계속합니다. ticker={}, range={}", ticker, chartRange, e);
      return null;
    }
  }

  private void saveChartSnapshot(
      Stock stock, StockChartRange chartRange, StockChartResDTO.Chart chart) {
    try {
      stockSnapshotPersistenceService.saveChart(stock, chartRange, chart);
    } catch (RuntimeException e) {
      log.warn(
          "종목 차트 DB snapshot 저장 실패를 무시합니다. ticker={}, range={}", stock.getTicker(), chartRange, e);
    }
  }

  private StockChartResDTO.Chart findCachedChart(
      String ticker, StockChartRange chartRange, StockChartType chartType) {
    try {
      return stockChartRedisService.find(ticker, chartRange, chartType);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, range={}, type={}",
          StockErrorCode.STOCK_CHART_CACHE_DESERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_CACHE_DESERIALIZE_FAILED.getMessage(),
          ticker,
          chartRange.getValue(),
          chartType,
          e);
      return null;
    }
  }

  private void saveChartCache(StockChartResDTO.Chart chart) {
    try {
      stockChartRedisService.save(chart);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, range={}, type={}",
          StockErrorCode.STOCK_CHART_CACHE_SERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_CACHE_SERIALIZE_FAILED.getMessage(),
          chart.ticker(),
          chart.range(),
          chart.type(),
          e);
    }
  }

  private void subscribeRealtimeIfNeeded(String ticker, StockChartRange range) {
    if (range == StockChartRange.ONE_DAY) {
      kisStockPriceWebSocketClient.subscribe(ticker);
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.STOCK_NOT_FOUND);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
