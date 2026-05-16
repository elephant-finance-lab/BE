package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockChartClient;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceWebSocketClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockChartRedisService;
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
public class StockChartQueryServiceImpl implements StockChartQueryService {

  private static final String CURRENCY_KRW = "KRW";

  private final StockRepository stockRepository;
  private final StockChartRedisService stockChartRedisService;
  private final KisStockChartClient kisStockChartClient;
  private final KisStockPriceWebSocketClient kisStockPriceWebSocketClient;

  @Override
  public StockChartResDTO.Chart getChart(String ticker, String range, String type) {
    StockChartRange chartRange = StockChartRange.from(range);
    StockChartType chartType = StockChartType.from(type);
    Stock stock =
        stockRepository
            .findByTicker(normalizeTicker(ticker))
            .orElseThrow(() -> new StockException(StockErrorCode.STOCK_NOT_FOUND));

    StockChartResDTO.Chart cachedChart = findCachedChart(stock, chartRange, chartType);
    if (cachedChart != null) {
      subscribeRealtimeIfNeeded(stock.getTicker(), chartRange);
      log.info("종목 차트 캐시 조회 성공. ticker={}, range={}, type={}", stock.getTicker(), range, type);
      return cachedChart;
    }

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
    saveChartCache(stock, chart);
    subscribeRealtimeIfNeeded(stock.getTicker(), chartRange);
    return chart;
  }

  private StockChartResDTO.Chart findCachedChart(
      Stock stock, StockChartRange chartRange, StockChartType chartType) {
    try {
      return stockChartRedisService.find(stock.getTicker(), chartRange, chartType);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, range={}, type={}",
          StockErrorCode.STOCK_CHART_CACHE_DESERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_CACHE_DESERIALIZE_FAILED.getMessage(),
          stock.getTicker(),
          chartRange.getValue(),
          chartType,
          e);
      return null;
    }
  }

  private void saveChartCache(Stock stock, StockChartResDTO.Chart chart) {
    try {
      stockChartRedisService.save(chart);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, range={}, type={}",
          StockErrorCode.STOCK_CHART_CACHE_SERIALIZE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_CACHE_SERIALIZE_FAILED.getMessage(),
          stock.getTicker(),
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
