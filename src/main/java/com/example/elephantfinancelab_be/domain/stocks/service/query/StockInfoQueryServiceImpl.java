package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockInfoPriceRedisService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSnapshotPersistenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StockInfoQueryServiceImpl implements StockInfoQueryService {

  private static final String INCOME_STATEMENT = "INCOME";
  private static final List<String> FINANCIAL_SUMMARY_LABELS = List.of("매출액", "영업 이익", "당기순이익");

  private final StockResolverService stockResolverService;
  private final StockInfoPriceRedisService stockInfoPriceRedisService;
  private final StockSnapshotPersistenceService stockSnapshotPersistenceService;
  private final KisStockPriceClient kisStockPriceClient;
  private final StockFinancialQueryService stockFinancialQueryService;

  @Override
  public Mono<StockInfoResDTO.Info> getInfo(String ticker, String period) {
    StockFinancialPeriod financialPeriod = StockFinancialPeriod.from(period);
    Mono<Stock> stockMono = blockingMono(() -> stockResolverService.resolve(ticker)).cache();
    Mono<StockInfoResDTO.Price> priceMono =
        stockMono.flatMap(stock -> blockingMono(() -> getPrice(stock)));
    Mono<StockFinancialResDTO.Financial> financialMono =
        blockingMono(
                () ->
                    stockFinancialQueryService.getFinancial(
                        ticker, INCOME_STATEMENT, financialPeriod.name()))
            .onErrorResume(
                StockException.class,
                e -> {
                  log.info(
                      "종목 정보의 재무 요약 조회를 생략합니다. ticker={}, code={}", ticker, e.getCode().getCode());
                  return Mono.just(emptyFinancial(ticker, financialPeriod));
                });

    return Mono.zip(stockMono, priceMono, financialMono)
        .map(tuple -> toInfo(tuple.getT1(), tuple.getT2(), tuple.getT3()));
  }

  private StockInfoResDTO.Info toInfo(
      Stock stock, StockInfoResDTO.Price price, StockFinancialResDTO.Financial financial) {
    return new StockInfoResDTO.Info(
        stock.getTicker(), stock.getName(), price, toFinancialSummary(financial));
  }

  private StockInfoResDTO.Price getPrice(Stock stock) {
    StockInfoResDTO.Price cachedPrice = findCachedPrice(stock.getTicker());
    if (cachedPrice != null) {
      log.info("종목 정보 시세 캐시 조회 성공. ticker={}", stock.getTicker());
      return cachedPrice;
    }

    StockInfoResDTO.Price storedPrice = findStoredPrice(stock.getTicker(), false);
    if (storedPrice != null) {
      savePriceCache(stock.getTicker(), storedPrice);
      log.info("종목 정보 시세 DB snapshot 조회 성공. ticker={}", stock.getTicker());
      return storedPrice;
    }

    try {
      StockInfoResDTO.Price price = kisStockPriceClient.fetchInfoPrice(stock);
      savePriceSnapshot(stock, price);
      savePriceCache(stock.getTicker(), price);
      return price;
    } catch (StockException e) {
      StockInfoResDTO.Price lastStoredPrice = findStoredPrice(stock.getTicker(), true);
      if (lastStoredPrice != null) {
        log.warn(
            "KIS 시세 조회 실패로 마지막 DB snapshot을 반환합니다. ticker={}, code={}",
            stock.getTicker(),
            e.getCode().getCode());
        savePriceCache(stock.getTicker(), lastStoredPrice);
        return lastStoredPrice;
      }
      throw e;
    }
  }

  private StockInfoResDTO.Price findCachedPrice(String ticker) {
    try {
      return stockInfoPriceRedisService.find(ticker);
    } catch (RuntimeException e) {
      log.warn("종목 정보 시세 Redis 조회 실패로 DB 조회를 계속합니다. ticker={}", ticker, e);
      return null;
    }
  }

  private StockInfoResDTO.Price findStoredPrice(String ticker, boolean includeStale) {
    try {
      return includeStale
          ? stockSnapshotPersistenceService.findLastInfoPrice(ticker)
          : stockSnapshotPersistenceService.findInfoPrice(ticker);
    } catch (RuntimeException e) {
      log.warn("종목 정보 시세 DB snapshot 조회 실패로 KIS 조회를 계속합니다. ticker={}", ticker, e);
      return null;
    }
  }

  private void savePriceCache(String ticker, StockInfoResDTO.Price price) {
    try {
      stockInfoPriceRedisService.save(ticker, price);
    } catch (RuntimeException e) {
      log.warn("종목 정보 시세 Redis 저장 실패를 무시합니다. ticker={}", ticker, e);
    }
  }

  private void savePriceSnapshot(Stock stock, StockInfoResDTO.Price price) {
    try {
      stockSnapshotPersistenceService.saveInfoPrice(stock, price);
    } catch (RuntimeException e) {
      log.warn("종목 정보 시세 DB snapshot 저장 실패를 무시합니다. ticker={}", stock.getTicker(), e);
    }
  }

  private StockFinancialResDTO.Financial emptyFinancial(
      String ticker, StockFinancialPeriod financialPeriod) {
    return new StockFinancialResDTO.Financial(
        ticker,
        null,
        StockFinancialStatement.INCOME,
        financialPeriod,
        StockFinancialStatement.INCOME.getUnit(),
        List.of(),
        List.of());
  }

  private StockInfoResDTO.FinancialSummary toFinancialSummary(
      StockFinancialResDTO.Financial financial) {
    List<StockFinancialResDTO.Row> financialRows = safeList(financial.rows());
    List<StockInfoResDTO.Row> rows =
        FINANCIAL_SUMMARY_LABELS.stream()
            .map(label -> findFinancialRow(financialRows, label))
            .filter(row -> row != null)
            .map(row -> new StockInfoResDTO.Row(row.label(), safeList(row.values())))
            .toList();
    return new StockInfoResDTO.FinancialSummary(
        financial.period(), financial.unit(), safeList(financial.columns()), rows);
  }

  private StockFinancialResDTO.Row findFinancialRow(
      List<StockFinancialResDTO.Row> rows, String label) {
    return rows.stream()
        .filter(row -> row != null && label.equals(row.label()))
        .findFirst()
        .orElse(null);
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private <T> Mono<T> blockingMono(BlockingSupplier<T> supplier) {
    return Mono.fromCallable(supplier::get).subscribeOn(Schedulers.boundedElastic());
  }

  @FunctionalInterface
  private interface BlockingSupplier<T> {

    T get() throws StockException;
  }
}
