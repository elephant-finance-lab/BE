package com.example.elephantfinancelab_be.domain.stocks.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockInfoPriceRedisService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSnapshotPersistenceService;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockInfoQueryServiceImplTest {

  private final Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
  private final StockResolverService stockResolverService = mock(StockResolverService.class);
  private final StockInfoPriceRedisService stockInfoPriceRedisService =
      mock(StockInfoPriceRedisService.class);
  private final StockSnapshotPersistenceService stockSnapshotPersistenceService =
      mock(StockSnapshotPersistenceService.class);
  private final KisStockPriceClient kisStockPriceClient = mock(KisStockPriceClient.class);
  private final StockFinancialQueryService stockFinancialQueryService =
      mock(StockFinancialQueryService.class);
  private final StockInfoQueryServiceImpl service =
      new StockInfoQueryServiceImpl(
          stockResolverService,
          stockInfoPriceRedisService,
          stockSnapshotPersistenceService,
          kisStockPriceClient,
          stockFinancialQueryService);

  @Test
  void returnsCachedQuotePriceAndThreeIncomeRows() {
    stubCachedPrice();
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(financial());

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.ticker()).isEqualTo("005930");
    assertThat(result.nameKor()).isEqualTo("삼성전자");
    assertThat(result.price().dayLowPriceKrw()).isEqualTo(69000L);
    assertThat(result.price().dayHighPriceKrw()).isEqualTo(73000L);
    assertThat(result.price().week52LowPriceKrw()).isEqualTo(58000L);
    assertThat(result.price().week52HighPriceKrw()).isEqualTo(82000L);
    assertThat(result.price().openPriceKrw()).isEqualTo(70000L);
    assertThat(result.price().currentPriceKrw()).isEqualTo(72500L);
    assertThat(result.price().volume()).isEqualTo(123456L);
    assertThat(result.price().tradingValueKrw()).isEqualTo(8900000000L);
    assertThat(result.price().asOfDate()).isEqualTo("2026-05-22");
    assertThat(result.financialSummary().columns()).containsExactly("24년 9월", "24년 12월", "25년 3월");
    assertThat(result.financialSummary().rows())
        .extracting(StockInfoResDTO.Row::label)
        .containsExactly("매출액", "영업 이익", "당기순이익");
  }

  @Test
  void fallsBackToEmptyFinancialListsWhenFinancialRowsOrColumnsAreNull() {
    stubCachedPrice();
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(
            new StockFinancialResDTO.Financial(
                "005930",
                "삼성전자",
                StockFinancialStatement.INCOME,
                StockFinancialPeriod.QUARTER,
                "억원",
                null,
                null));

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.financialSummary().columns()).isEmpty();
    assertThat(result.financialSummary().rows()).isEmpty();
  }

  @Test
  void ordersFinancialRowsBySummaryLabelOrder() {
    stubCachedPrice();
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(shuffledFinancial());

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.financialSummary().rows())
        .extracting(StockInfoResDTO.Row::label)
        .containsExactly("매출액", "영업 이익", "당기순이익");
  }

  @Test
  void returnsPriceWithoutFinancialDataWhenFinancialRequestFails() {
    stubCachedPrice();
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenThrow(new StockException(StockErrorCode.KIS_STOCK_FINANCIAL_API_FAILED));

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.price().dayLowPriceKrw()).isEqualTo(69000L);
    assertThat(result.price().week52HighPriceKrw()).isEqualTo(82000L);
    assertThat(result.financialSummary().rows()).isEmpty();
  }

  @Test
  void fetchesQuoteAndPersistsItWhenCacheAndDatabaseAreEmpty() {
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchInfoPrice(stock)).thenReturn(price());
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(financial());

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.price()).isEqualTo(price());
    verify(stockSnapshotPersistenceService).saveInfoPrice(stock, price());
    verify(stockInfoPriceRedisService).save("005930", price());
  }

  private void stubCachedPrice() {
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(stockInfoPriceRedisService.find("005930")).thenReturn(price());
  }

  private StockInfoResDTO.Price price() {
    return new StockInfoResDTO.Price(
        69000L, 73000L, 58000L, 82000L, 70000L, 72500L, 123456L, 8900000000L, "2026-05-22");
  }

  private StockFinancialResDTO.Financial financial() {
    return new StockFinancialResDTO.Financial(
        "005930",
        "삼성전자",
        StockFinancialStatement.INCOME,
        StockFinancialPeriod.QUARTER,
        "억원",
        List.of("24년 9월", "24년 12월", "25년 3월"),
        List.of(
            new StockFinancialResDTO.Row("매출액", List.of("100", "200", "300")),
            new StockFinancialResDTO.Row("매출 원가", List.of("10", "20", "30")),
            new StockFinancialResDTO.Row("매출 총 이익", List.of("40", "50", "60")),
            new StockFinancialResDTO.Row("영업 이익", List.of("45", "55", "65")),
            new StockFinancialResDTO.Row("당기순이익", List.of("70", "80", "90"))));
  }

  private StockFinancialResDTO.Financial shuffledFinancial() {
    return new StockFinancialResDTO.Financial(
        "005930",
        "삼성전자",
        StockFinancialStatement.INCOME,
        StockFinancialPeriod.QUARTER,
        "억원",
        List.of("24년 9월", "24년 12월", "25년 3월"),
        List.of(
            new StockFinancialResDTO.Row("당기순이익", List.of("70", "80", "90")),
            new StockFinancialResDTO.Row("매출 총 이익", List.of("40", "50", "60")),
            new StockFinancialResDTO.Row("영업 이익", List.of("45", "55", "65")),
            new StockFinancialResDTO.Row("매출액", List.of("100", "200", "300"))));
  }
}
