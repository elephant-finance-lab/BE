package com.example.elephantfinancelab_be.domain.stocks.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialApiType;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockFinancialClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockFinancialRedisService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSnapshotPersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockFinancialQueryServiceImplTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final StockResolverService stockResolverService = mock(StockResolverService.class);
  private final StockFinancialRedisService stockFinancialRedisService =
      mock(StockFinancialRedisService.class);
  private final StockSnapshotPersistenceService stockSnapshotPersistenceService =
      mock(StockSnapshotPersistenceService.class);
  private final KisStockFinancialClient kisStockFinancialClient =
      mock(KisStockFinancialClient.class);
  private final StockFinancialQueryServiceImpl service =
      new StockFinancialQueryServiceImpl(
          stockResolverService,
          stockFinancialRedisService,
          stockSnapshotPersistenceService,
          kisStockFinancialClient);

  @Test
  void convertsQuarterlyIncomeStatementFromCumulativeToQuarterlyValues() {
    Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(stockFinancialRedisService.find(any(), any(), any())).thenReturn(null);
    when(kisStockFinancialClient.fetchFinancial(
            "005930", StockFinancialApiType.INCOME, StockFinancialPeriod.QUARTER))
        .thenReturn(
            List.of(
                incomeNode("202503", "791405", "66853", "99.99"),
                incomeNode("202506", "1537068", "113613", "99.99"),
                incomeNode("202509", "2397686", "235274", "99.99"),
                incomeNode("202512", "3336059", "436011", "99.99")));

    StockFinancialResDTO.Financial result = service.getFinancial("005930", "INCOME", "QUARTER");

    assertThat(result.unit()).isEqualTo("억원");
    assertThat(result.columns()).containsExactly("25년 1Q", "25년 2Q", "25년 3Q", "25년 4Q");
    assertThat(rowValues(result, "매출액")).containsExactly("791405", "745663", "860618", "938373");
    assertThat(rowValues(result, "영업 이익")).containsExactly("66853", "46760", "121661", "200737");
    assertThat(result.rows())
        .extracting(StockFinancialResDTO.Row::label)
        .doesNotContain("감가상각비", "판매 및 관리비", "영업 외 수익", "영업 외 비용", "특별 이익", "특별 손실");
  }

  @Test
  void doesNotSubtractIncomeValuesAcrossDifferentYears() {
    Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(stockFinancialRedisService.find(any(), any(), any())).thenReturn(null);
    when(kisStockFinancialClient.fetchFinancial(
            "005930", StockFinancialApiType.INCOME, StockFinancialPeriod.QUARTER))
        .thenReturn(
            List.of(
                incomeNode("202412", "3000000", "400000", "99.99"),
                incomeNode("202503", "700000", "100000", "99.99"),
                incomeNode("202506", "1500000", "250000", "99.99")));

    StockFinancialResDTO.Financial result = service.getFinancial("005930", "INCOME", "QUARTER");

    assertThat(rowValues(result, "매출액")).containsExactly("3000000", "700000", "800000");
  }

  @Test
  void returnsCachedPeriodResponseWithoutResolvingStockOrCallingKis() {
    StockFinancialResDTO.Financial cached =
        new StockFinancialResDTO.Financial(
            "005930",
            "삼성전자",
            StockFinancialStatement.INCOME,
            StockFinancialPeriod.YEAR,
            "억원",
            List.of("25년"),
            List.of());
    when(stockFinancialRedisService.find(
            "005930", StockFinancialStatement.INCOME, StockFinancialPeriod.YEAR))
        .thenReturn(cached);

    StockFinancialResDTO.Financial result = service.getFinancial("005930", "INCOME", "YEAR");

    assertThat(result).isEqualTo(cached);
    verifyNoInteractions(
        stockResolverService, stockSnapshotPersistenceService, kisStockFinancialClient);
  }

  @Test
  void returnsDatabaseSnapshotWithoutCallingKisWhenRedisIsEmpty() {
    Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
    StockFinancialResDTO.Financial stored =
        new StockFinancialResDTO.Financial(
            "005930",
            "삼성전자",
            StockFinancialStatement.INCOME,
            StockFinancialPeriod.QUARTER,
            "억원",
            List.of("25년 1Q"),
            List.of());
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(stockSnapshotPersistenceService.findFinancial(
            "005930", StockFinancialStatement.INCOME, StockFinancialPeriod.QUARTER))
        .thenReturn(stored);

    StockFinancialResDTO.Financial result = service.getFinancial("005930", "INCOME", "QUARTER");

    assertThat(result).isEqualTo(stored);
    verifyNoInteractions(kisStockFinancialClient);
  }

  private List<String> rowValues(StockFinancialResDTO.Financial financial, String label) {
    return financial.rows().stream()
        .filter(row -> row.label().equals(label))
        .findFirst()
        .orElseThrow()
        .values();
  }

  private JsonNode incomeNode(
      String periodKey, String saleAccount, String operatingProfit, String depreciationCost) {
    return objectMapper
        .createObjectNode()
        .put("stac_yymm", periodKey)
        .put("sale_account", saleAccount)
        .put("sale_cost", "0")
        .put("sale_totl_prfi", "0")
        .put("depr_cost", depreciationCost)
        .put("sell_mang", "0")
        .put("bsop_prti", operatingProfit)
        .put("bsop_non_ernn", "0")
        .put("bsop_non_expn", "0")
        .put("op_prfi", "0")
        .put("spec_prfi", "0")
        .put("spec_loss", "0")
        .put("thtr_ntin", "0");
  }
}
