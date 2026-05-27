package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockDailyPriceResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartSnapshot;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockDailyPriceSnapshot;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialSnapshot;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockInfoPriceSnapshot;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockChartSnapshotRepository;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockDailyPriceSnapshotRepository;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockFinancialSnapshotRepository;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockInfoPriceSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockSnapshotPersistenceService {

  private static final Duration DAILY_PRICE_MAX_AGE = Duration.ofMinutes(5);
  private static final Duration INFO_PRICE_MAX_AGE = Duration.ofMinutes(5);
  private static final Duration ONE_DAY_CHART_MAX_AGE = Duration.ofSeconds(30);
  private static final Duration FINANCIAL_MAX_AGE = Duration.ofHours(24);

  private final StockDailyPriceSnapshotRepository stockDailyPriceSnapshotRepository;
  private final StockChartSnapshotRepository stockChartSnapshotRepository;
  private final StockFinancialSnapshotRepository stockFinancialSnapshotRepository;
  private final StockInfoPriceSnapshotRepository stockInfoPriceSnapshotRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public StockInfoResDTO.Price findInfoPrice(String ticker) {
    return stockInfoPriceSnapshotRepository
        .findByStock_Ticker(ticker)
        .filter(snapshot -> isFresh(snapshot.getUpdatedAt(), INFO_PRICE_MAX_AGE))
        .map(snapshot -> read(snapshot.getResponseJson(), StockInfoResDTO.Price.class))
        .orElse(null);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public StockInfoResDTO.Price findLastInfoPrice(String ticker) {
    return stockInfoPriceSnapshotRepository
        .findByStock_Ticker(ticker)
        .map(snapshot -> read(snapshot.getResponseJson(), StockInfoResDTO.Price.class))
        .orElse(null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveInfoPrice(Stock stock, StockInfoResDTO.Price price) {
    String responseJson = write(price);
    StockInfoPriceSnapshot snapshot =
        stockInfoPriceSnapshotRepository
            .findByStock_Ticker(stock.getTicker())
            .orElseGet(
                () ->
                    StockInfoPriceSnapshot.builder()
                        .stock(stock)
                        .responseJson(responseJson)
                        .build());
    snapshot.updateResponseJson(responseJson);
    stockInfoPriceSnapshotRepository.save(snapshot);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public StockDailyPriceResDTO.DailyPrices findDailyPrices(String ticker) {
    return stockDailyPriceSnapshotRepository
        .findByStock_Ticker(ticker)
        .filter(snapshot -> isFresh(snapshot.getUpdatedAt(), DAILY_PRICE_MAX_AGE))
        .map(snapshot -> read(snapshot.getResponseJson(), StockDailyPriceResDTO.DailyPrices.class))
        .orElse(null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveDailyPrices(Stock stock, StockDailyPriceResDTO.DailyPrices dailyPrices) {
    String responseJson = write(dailyPrices);
    StockDailyPriceSnapshot snapshot =
        stockDailyPriceSnapshotRepository
            .findByStock_Ticker(stock.getTicker())
            .orElseGet(
                () ->
                    StockDailyPriceSnapshot.builder()
                        .stock(stock)
                        .responseJson(responseJson)
                        .build());
    snapshot.updateResponseJson(responseJson);
    stockDailyPriceSnapshotRepository.save(snapshot);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public StockChartResDTO.Chart findChart(
      String ticker, StockChartRange range, StockChartType requestedType) {
    return stockChartSnapshotRepository
        .findByStock_TickerAndChartRange(ticker, range)
        .filter(snapshot -> isFresh(snapshot.getUpdatedAt(), chartMaxAge(range)))
        .map(snapshot -> read(snapshot.getResponseJson(), StockChartResDTO.Chart.class))
        .map(
            stored ->
                new StockChartResDTO.Chart(
                    stored.ticker(),
                    stored.range(),
                    requestedType,
                    stored.interval(),
                    stored.currency(),
                    stored.data()))
        .orElse(null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveChart(Stock stock, StockChartRange range, StockChartResDTO.Chart chart) {
    String responseJson = write(chart);
    StockChartSnapshot snapshot =
        stockChartSnapshotRepository
            .findByStock_TickerAndChartRange(stock.getTicker(), range)
            .orElseGet(
                () ->
                    StockChartSnapshot.builder()
                        .stock(stock)
                        .chartRange(range)
                        .responseJson(responseJson)
                        .build());
    snapshot.updateResponseJson(responseJson);
    stockChartSnapshotRepository.save(snapshot);
  }

  @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
  public StockFinancialResDTO.Financial findFinancial(
      String ticker, StockFinancialStatement statement, StockFinancialPeriod period) {
    return stockFinancialSnapshotRepository
        .findByStock_TickerAndStatementAndPeriod(ticker, statement, period)
        .filter(snapshot -> isFresh(snapshot.getUpdatedAt(), FINANCIAL_MAX_AGE))
        .map(snapshot -> read(snapshot.getResponseJson(), StockFinancialResDTO.Financial.class))
        .orElse(null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveFinancial(Stock stock, StockFinancialResDTO.Financial financial) {
    String responseJson = write(financial);
    StockFinancialSnapshot snapshot =
        stockFinancialSnapshotRepository
            .findByStock_TickerAndStatementAndPeriod(
                stock.getTicker(), financial.statement(), financial.period())
            .orElseGet(
                () ->
                    StockFinancialSnapshot.builder()
                        .stock(stock)
                        .statement(financial.statement())
                        .period(financial.period())
                        .responseJson(responseJson)
                        .build());
    snapshot.updateResponseJson(responseJson);
    stockFinancialSnapshotRepository.save(snapshot);
  }

  private String write(Object response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_SNAPSHOT_SERIALIZE_FAILED, e);
    }
  }

  private <T> T read(String json, Class<T> responseType) {
    try {
      return objectMapper.readValue(json, responseType);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_SNAPSHOT_DESERIALIZE_FAILED, e);
    }
  }

  private boolean isFresh(LocalDateTime updatedAt, Duration maxAge) {
    return updatedAt != null && updatedAt.isAfter(LocalDateTime.now().minus(maxAge));
  }

  private Duration chartMaxAge(StockChartRange range) {
    return switch (range) {
      case ONE_DAY -> ONE_DAY_CHART_MAX_AGE;
      case ONE_WEEK -> Duration.ofMinutes(5);
      case THREE_MONTHS -> Duration.ofHours(1);
      case ONE_YEAR -> Duration.ofHours(6);
      case FIVE_YEARS, ALL -> Duration.ofHours(24);
    };
  }
}
