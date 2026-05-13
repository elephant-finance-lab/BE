package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockChartRealtimeService {

  private static final DateTimeFormatter MINUTE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final StockChartRedisService stockChartRedisService;
  private final SimpMessagingTemplate messagingTemplate;

  public void updateAndPush(String ticker, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    Flux.fromArray(StockChartType.values())
        .flatMap(
            type ->
                Mono.justOrEmpty(
                    stockChartRedisService.find(ticker, StockChartRange.ONE_DAY, type)))
        .map(chart -> updateChart(chart, parsed))
        .doOnNext(stockChartRedisService::save)
        .doOnNext(this::push)
        .doOnError(
            error ->
                log.warn(
                    "code={}, message={}, ticker={}",
                    StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getCode(),
                    StockErrorCode.STOCK_CHART_REALTIME_UPDATE_FAILED.getMessage(),
                    ticker,
                    error))
        .subscribe();
  }

  private StockChartResDTO.Chart updateChart(
      StockChartResDTO.Chart chart, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    List<StockChartResDTO.DataPoint> data = new ArrayList<>(chart.data());
    String minuteTime = minuteTime(parsed.updatedAt());
    StockChartResDTO.DataPoint realtimePoint = newPoint(minuteTime, parsed);

    if (data.isEmpty()) {
      data.add(realtimePoint);
      return chartWithData(chart, data);
    }

    int lastIndex = data.size() - 1;
    StockChartResDTO.DataPoint lastPoint = data.get(lastIndex);
    if (minuteTime.equals(lastPoint.time())) {
      data.set(lastIndex, mergePoint(chart.type(), lastPoint, parsed));
    } else {
      data.add(realtimePoint);
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

  private void push(StockChartResDTO.Chart chart) {
    try {
      messagingTemplate.convertAndSend(destination(chart.ticker()), chart);
      log.debug("종목 1D 차트 실시간 push 완료. ticker={}, type={}", chart.ticker(), chart.type());
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_CHART_PUSH_FAILED.getCode(),
          StockErrorCode.STOCK_CHART_PUSH_FAILED.getMessage(),
          chart.ticker(),
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
}
