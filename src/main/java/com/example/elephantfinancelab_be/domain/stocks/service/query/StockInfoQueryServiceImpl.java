package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockInfoQueryServiceImpl implements StockInfoQueryService {

  private static final String LINE_CHART_TYPE = "LINE";
  private static final String ONE_DAY_RANGE = "1D";
  private static final String ONE_YEAR_RANGE = "1Y";
  private static final String INCOME_STATEMENT = "INCOME";
  private static final List<String> FINANCIAL_SUMMARY_LABELS = List.of("매출액", "매출 총 이익", "당기순이익");

  private final StockChartQueryService stockChartQueryService;
  private final StockFinancialQueryService stockFinancialQueryService;

  @Override
  public StockInfoResDTO.Info getInfo(String ticker, String period) {
    StockFinancialPeriod financialPeriod = StockFinancialPeriod.from(period);

    // 변경내용: Mono.zip 병렬 조회 제거, 순차 호출로 변경
    StockChartResDTO.Chart oneDayChart =
        stockChartQueryService.getChart(ticker, ONE_DAY_RANGE, LINE_CHART_TYPE);
    StockChartResDTO.Chart oneYearChart =
        stockChartQueryService.getChart(ticker, ONE_YEAR_RANGE, LINE_CHART_TYPE);
    StockFinancialResDTO.Financial financial =
        stockFinancialQueryService.getFinancial(ticker, INCOME_STATEMENT, financialPeriod.name());

    return toInfo(oneDayChart, oneYearChart, financial);
  }

  private StockInfoResDTO.Info toInfo(
      StockChartResDTO.Chart oneDayChart,
      StockChartResDTO.Chart oneYearChart,
      StockFinancialResDTO.Financial financial) {
    return new StockInfoResDTO.Info(
        financial.ticker(),
        financial.nameKor(),
        toPrice(oneDayChart, oneYearChart),
        toFinancialSummary(financial));
  }

  private StockInfoResDTO.Price toPrice(
      StockChartResDTO.Chart oneDayChart, StockChartResDTO.Chart oneYearChart) {
    List<StockChartResDTO.DataPoint> oneDayData = oneDayChart.data();
    StockChartResDTO.DataPoint firstPoint = firstPoint(oneDayData);
    StockChartResDTO.DataPoint lastPoint = lastPoint(oneDayData);

    return new StockInfoResDTO.Price(
        minLowPrice(oneDayData),
        maxHighPrice(oneDayData),
        minLowPrice(oneYearChart.data()),
        maxHighPrice(oneYearChart.data()),
        firstPoint == null ? null : firstPoint.open(),
        lastPoint == null ? null : lastPoint.close());
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

  private Long minLowPrice(List<StockChartResDTO.DataPoint> data) {
    if (data == null || data.isEmpty()) return null;
    return data.stream()
        .map(StockChartResDTO.DataPoint::low)
        .filter(value -> value != null)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  private Long maxHighPrice(List<StockChartResDTO.DataPoint> data) {
    if (data == null || data.isEmpty()) return null;
    return data.stream()
        .map(StockChartResDTO.DataPoint::high)
        .filter(value -> value != null)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  private StockChartResDTO.DataPoint firstPoint(List<StockChartResDTO.DataPoint> data) {
    if (data == null || data.isEmpty()) return null;
    return data.get(0);
  }

  private StockChartResDTO.DataPoint lastPoint(List<StockChartResDTO.DataPoint> data) {
    if (data == null || data.isEmpty()) return null;
    return data.get(data.size() - 1);
  }

  private <T> List<T> safeList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
