package com.example.elephantfinancelab_be.domain.stocks.entity;

import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockFinancialStatement {
  INCOME(
      "백만원",
      List.of(
          new Metric(StockFinancialApiType.INCOME, "매출액", "sale_account"),
          new Metric(StockFinancialApiType.INCOME, "매출 원가", "sale_cost"),
          new Metric(StockFinancialApiType.INCOME, "매출 총 이익", "sale_totl_prfi"),
          new Metric(StockFinancialApiType.INCOME, "감가상각비", "depr_cost"),
          new Metric(StockFinancialApiType.INCOME, "판매 및 관리비", "sell_mang"),
          new Metric(StockFinancialApiType.INCOME, "영업 이익", "bsop_prti"),
          new Metric(StockFinancialApiType.INCOME, "영업 외 수익", "bsop_non_ernn"),
          new Metric(StockFinancialApiType.INCOME, "영업 외 비용", "bsop_non_expn"),
          new Metric(StockFinancialApiType.INCOME, "경상 이익", "op_prfi"),
          new Metric(StockFinancialApiType.INCOME, "특별 이익", "spec_prfi"),
          new Metric(StockFinancialApiType.INCOME, "특별 손실", "spec_loss"),
          new Metric(StockFinancialApiType.INCOME, "당기순이익", "thtr_ntin"))),
  BALANCE(
      "백만원",
      List.of(
          new Metric(StockFinancialApiType.BALANCE, "유동자산", "cras"),
          new Metric(StockFinancialApiType.BALANCE, "고정자산", "fxas"),
          new Metric(StockFinancialApiType.BALANCE, "자산총계", "total_aset"),
          new Metric(StockFinancialApiType.BALANCE, "유동부채", "flow_lblt"),
          new Metric(StockFinancialApiType.BALANCE, "고정부채", "fix_lblt"),
          new Metric(StockFinancialApiType.BALANCE, "부채총계", "total_lblt"),
          new Metric(StockFinancialApiType.BALANCE, "자본금", "cpfn"),
          new Metric(StockFinancialApiType.BALANCE, "자본 잉여금", "cfp_surp"),
          new Metric(StockFinancialApiType.BALANCE, "이익 잉여금", "prfi_surp"),
          new Metric(StockFinancialApiType.BALANCE, "자본총계", "total_cptl"))),
  RATIO(
      "혼합",
      List.of(
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "매출액 증가율", "grs"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "영업 이익 증가율", "bsop_prfi_inrt"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "순이익 증가율", "ntin_inrt"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "ROE", "roe_val"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "EPS", "eps"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "주당매출액", "sps"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "BPS", "bps"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "유보 비율", "rsrv_rate"),
          new Metric(StockFinancialApiType.FINANCIAL_RATIO, "부채 비율", "lblt_rate"),
          new Metric(StockFinancialApiType.PROFIT_RATIO, "총자본 순이익율", "cptl_ntin_rate"),
          new Metric(StockFinancialApiType.PROFIT_RATIO, "자기자본 순이익율", "self_cptl_ntin_inrt"),
          new Metric(StockFinancialApiType.PROFIT_RATIO, "매출액 순이익율", "sale_ntin_rate"),
          new Metric(StockFinancialApiType.PROFIT_RATIO, "매출액 총이익율", "sale_totl_rate")));

  private final String unit;
  private final List<Metric> metrics;

  public static StockFinancialStatement from(String value) {
    if (value == null || value.isBlank()) {
      throw new StockException(StockErrorCode.INVALID_STOCK_FINANCIAL_STATEMENT);
    }

    try {
      return StockFinancialStatement.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new StockException(StockErrorCode.INVALID_STOCK_FINANCIAL_STATEMENT);
    }
  }

  public record Metric(StockFinancialApiType apiType, String label, String fieldName) {}
}
