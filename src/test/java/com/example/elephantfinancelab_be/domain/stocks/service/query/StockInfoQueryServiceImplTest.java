package com.example.elephantfinancelab_be.domain.stocks.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartInterval;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockInfoQueryServiceImplTest {

  private final StockChartQueryService stockChartQueryService = mock(StockChartQueryService.class);
  private final StockFinancialQueryService stockFinancialQueryService =
      mock(StockFinancialQueryService.class);
  private final StockInfoQueryServiceImpl service =
      new StockInfoQueryServiceImpl(stockChartQueryService, stockFinancialQueryService);

  @Test
  void aggregatesPriceAndThreeIncomeRows() {
    when(stockChartQueryService.getChart("005930", "1D", "LINE")).thenReturn(oneDayChart());
    when(stockChartQueryService.getChart("005930", "1Y", "LINE")).thenReturn(oneYearChart());
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(financial());

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.ticker()).isEqualTo("005930");
    assertThat(result.nameKor()).isEqualTo("삼성전자");
    assertThat(result.price().dayLowPriceKrw()).isEqualTo(69000L);
    assertThat(result.price().dayHighPriceKrw()).isEqualTo(73000L);
    assertThat(result.price().yearLowPriceKrw()).isEqualTo(58000L);
    assertThat(result.price().yearHighPriceKrw()).isEqualTo(82000L);
    assertThat(result.price().openPriceKrw()).isEqualTo(70000L);
    assertThat(result.price().closePriceKrw()).isEqualTo(72500L);
    assertThat(result.financialSummary().columns()).containsExactly("24년 9월", "24년 12월", "25년 3월");
    assertThat(result.financialSummary().rows())
        .extracting(StockInfoResDTO.Row::label)
        .containsExactly("매출액", "매출 총 이익", "당기순이익");
  }

  @Test
  void fallsBackToEmptyFinancialListsWhenFinancialRowsOrColumnsAreNull() {
    when(stockChartQueryService.getChart("005930", "1D", "LINE")).thenReturn(oneDayChart());
    when(stockChartQueryService.getChart("005930", "1Y", "LINE")).thenReturn(oneYearChart());
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
    when(stockChartQueryService.getChart("005930", "1D", "LINE")).thenReturn(oneDayChart());
    when(stockChartQueryService.getChart("005930", "1Y", "LINE")).thenReturn(oneYearChart());
    when(stockFinancialQueryService.getFinancial("005930", "INCOME", "QUARTER"))
        .thenReturn(shuffledFinancial());

    StockInfoResDTO.Info result = service.getInfo("005930", "QUARTER").block();

    assertThat(result.financialSummary().rows())
        .extracting(StockInfoResDTO.Row::label)
        .containsExactly("매출액", "매출 총 이익", "당기순이익");
  }

  private StockChartResDTO.Chart oneDayChart() {
    return new StockChartResDTO.Chart(
        "005930",
        "1D",
        StockChartType.LINE,
        StockChartInterval.MINUTE,
        "KRW",
        List.of(
            new StockChartResDTO.DataPoint(
                "2026-05-22T09:00:00", 70000L, 70000L, 71000L, 69000L, 70500L, 1000L),
            new StockChartResDTO.DataPoint(
                "2026-05-22T15:30:00", 72500L, 70500L, 73000L, 70000L, 72500L, 2000L)));
  }

  private StockChartResDTO.Chart oneYearChart() {
    return new StockChartResDTO.Chart(
        "005930",
        "1Y",
        StockChartType.LINE,
        StockChartInterval.DAY,
        "KRW",
        List.of(
            new StockChartResDTO.DataPoint(
                "2025-05-22", 61000L, 60000L, 62000L, 58000L, 61000L, 10000L),
            new StockChartResDTO.DataPoint(
                "2026-05-22", 72500L, 70000L, 82000L, 69000L, 72500L, 20000L)));
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
            new StockFinancialResDTO.Row("매출액", List.of("100", "200", "300"))));
  }
}
