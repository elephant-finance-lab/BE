package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockChartRealtimeService {

  private static final DateTimeFormatter MINUTE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final Duration UPDATE_LOCK_TTL = Duration.ofSeconds(3);
  private static final Duration UPDATE_LOCK_WAIT_TIMEOUT = Duration.ofSeconds(1);
  private static final Duration UPDATE_LOCK_RETRY_INTERVAL = Duration.ofMillis(20);
  private static final Duration INACTIVE_STATE_RETRY_INTERVAL = Duration.ofSeconds(1);

  private final StockChartRedisService stockChartRedisService;
  private final SimpMessagingTemplate messagingTemplate;
  private final Map<ChartKey, ChartRuntimeState> chartStates = new ConcurrentHashMap<>();

  public void updateAndPush(String ticker, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    for (StockChartType type : StockChartType.values()) {
      updateRuntimeStateAndPush(ticker, type, parsed);
    }
  }

  @Scheduled(fixedDelayString = "${app.stock-chart-realtime.flush-interval-ms:1000}")
  public void flushDirtyCharts() {
    chartStates.forEach((key, state) -> flushDirtyChart(key, state));
  }

  private void updateRuntimeStateAndPush(
      String ticker, StockChartType type, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    try {
      ChartKey key = new ChartKey(normalizeTicker(ticker), type);
      ChartRuntimeState state = chartStates.computeIfAbsent(key, this::loadRuntimeState);
      if (state == null || !state.active()) {
        if (state != null && state.retryable()) {
          chartStates.remove(key, state);
        }
        return;
      }

      StockChartResDTO.Update update = state.update(parsed);
      push(update);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, type={}",
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
          ticker,
          type,
          e);
    }
  }

  private ChartRuntimeState loadRuntimeState(ChartKey key) {
    try {
      StockChartResDTO.Chart chart =
          stockChartRedisService.find(key.ticker(), StockChartRange.ONE_DAY, key.type());
      if (chart == null) {
        return new ChartRuntimeState(false, null);
      }
      return new ChartRuntimeState(true, chart);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, type={}",
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
          key.ticker(),
          key.type(),
          e);
      return new ChartRuntimeState(false, null);
    }
  }

  private void flushDirtyChart(ChartKey key, ChartRuntimeState state) {
    StockChartResDTO.DataPoint point = state.dirtySnapshot();
    if (point == null) {
      return;
    }

    String lockToken = UUID.randomUUID().toString();
    try {
      if (!acquireUpdateLock(key.ticker(), key.type(), lockToken)) {
        log.warn(
            "code={}, message={}, ticker={}, type={}, reason=lock-timeout",
            StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
            StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
            key.ticker(),
            key.type());
        return;
      }

      StockChartResDTO.Chart chart =
          stockChartRedisService.find(key.ticker(), StockChartRange.ONE_DAY, key.type());
      if (chart == null) {
        chartStates.remove(key);
        return;
      }

      StockChartResDTO.Chart updatedChart = upsertPoint(chart, point);
      stockChartRedisService.save(updatedChart);
      state.markFlushed(point);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, type={}",
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
          key.ticker(),
          key.type(),
          e);
    } finally {
      releaseUpdateLock(key.ticker(), key.type(), lockToken);
    }
  }

  private boolean acquireUpdateLock(String ticker, StockChartType type, String lockToken) {
    long deadline = System.nanoTime() + UPDATE_LOCK_WAIT_TIMEOUT.toNanos();
    while (System.nanoTime() <= deadline) {
      if (stockChartRedisService.acquireUpdateLock(
          ticker, StockChartRange.ONE_DAY, type, lockToken, UPDATE_LOCK_TTL)) {
        return true;
      }

      try {
        Thread.sleep(UPDATE_LOCK_RETRY_INTERVAL.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  private void releaseUpdateLock(String ticker, StockChartType type, String lockToken) {
    try {
      stockChartRedisService.releaseUpdateLock(ticker, StockChartRange.ONE_DAY, type, lockToken);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}, type={}, reason=lock-release-failed",
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
          ticker,
          type,
          e);
    }
  }

  private StockChartResDTO.Chart upsertPoint(
      StockChartResDTO.Chart chart, StockChartResDTO.DataPoint point) {
    List<StockChartResDTO.DataPoint> data = new ArrayList<>(chart.data());
    if (data.isEmpty()) {
      data.add(point);
      return chartWithData(chart, data);
    }

    int lastIndex = data.size() - 1;
    StockChartResDTO.DataPoint lastPoint = data.get(lastIndex);
    if (point.time().equals(lastPoint.time())) {
      data.set(lastIndex, point);
    } else {
      data.add(point);
    }

    return chartWithData(chart, data);
  }

  private StockChartResDTO.DataPoint mergePoint(
      StockChartType type,
      StockChartResDTO.DataPoint previous,
      StockPriceRealtimeParser.ParsedStockPrice parsed) {
    Long price = parsed.currentPriceKrw();
    if (type == StockChartType.LINE) {
      return new StockChartResDTO.DataPoint(
          previous.time(),
          price,
          previous.open(),
          previous.high(),
          previous.low(),
          price,
          previous.volume());
    }

    return new StockChartResDTO.DataPoint(
        previous.time(),
        price,
        previous.open(),
        max(previous.high(), price),
        min(previous.low(), price),
        price,
        safeLong(previous.volume()) + safeLong(parsed.tradeVolume()));
  }

  private StockChartResDTO.DataPoint newPoint(
      String minuteTime, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    Long price = parsed.currentPriceKrw();
    return new StockChartResDTO.DataPoint(
        minuteTime, price, price, price, price, price, safeLong(parsed.tradeVolume()));
  }

  private StockChartResDTO.Chart chartWithData(
      StockChartResDTO.Chart chart, List<StockChartResDTO.DataPoint> data) {
    return new StockChartResDTO.Chart(
        chart.ticker(), chart.range(), chart.type(), chart.interval(), chart.currency(), data);
  }

  private void push(StockChartResDTO.Update update) {
    try {
      messagingTemplate.convertAndSend(destination(update.ticker()), update);
      log.debug("종목 1D 차트 실시간 push 완료. ticker={}, type={}", update.ticker(), update.type());
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_CHART_PUSH_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_PUSH_FAILED.getMessage(),
          update.ticker(),
          e);
    }
  }

  private String destination(String ticker) {
    return "/topic/stocks/" + ticker.trim().toUpperCase(Locale.ROOT) + "/chart";
  }

  private String minuteTime(LocalDateTime updatedAt) {
    return updatedAt.withSecond(0).withNano(0).format(MINUTE_FORMATTER);
  }

  private Long max(Long left, Long right) {
    return Math.max(safeLong(left), safeLong(right));
  }

  private Long min(Long left, Long right) {
    if (left == null || left == 0L) {
      return safeLong(right);
    }
    return Math.min(left, safeLong(right));
  }

  private Long safeLong(Long value) {
    return value == null ? 0L : value;
  }

  private String normalizeTicker(String ticker) {
    return ticker.trim().toUpperCase(Locale.ROOT);
  }

  private record ChartKey(String ticker, StockChartType type) {}

  private final class ChartRuntimeState {

    private final boolean active;
    private final StockChartResDTO.Chart chart;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final long nextLoadAtNanos;
    private StockChartResDTO.DataPoint latestPoint;

    private ChartRuntimeState(boolean active, StockChartResDTO.Chart chart) {
      this.active = active;
      this.chart = chart;
      this.nextLoadAtNanos =
          active ? Long.MAX_VALUE : System.nanoTime() + INACTIVE_STATE_RETRY_INTERVAL.toNanos();
      this.latestPoint = chart == null || chart.data().isEmpty() ? null : chart.data().getLast();
    }

    private boolean active() {
      return active;
    }

    private boolean retryable() {
      return System.nanoTime() >= nextLoadAtNanos;
    }

    private synchronized StockChartResDTO.Update update(
        StockPriceRealtimeParser.ParsedStockPrice parsed) {
      String currentMinute = minuteTime(parsed.updatedAt());
      if (latestPoint == null || !currentMinute.equals(latestPoint.time())) {
        latestPoint = newPoint(currentMinute, parsed);
      } else {
        latestPoint = mergePoint(chart.type(), latestPoint, parsed);
      }
      dirty.set(true);
      return new StockChartResDTO.Update(
          chart.ticker(),
          chart.range(),
          chart.type(),
          chart.interval(),
          chart.currency(),
          latestPoint);
    }

    private synchronized StockChartResDTO.DataPoint dirtySnapshot() {
      return dirty.get() ? latestPoint : null;
    }

    private synchronized void markFlushed(StockChartResDTO.DataPoint flushedPoint) {
      if (latestPoint != null && latestPoint.equals(flushedPoint)) {
        dirty.set(false);
      }
    }
  }
}
