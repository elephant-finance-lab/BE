package com.example.elephantfinancelab_be.domain.chart.entity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public enum RankingType {
  VOLUME(
      "volume",
      "/uapi/domestic-stock/v1/quotations/volume-rank",
      "FHPST01710000",
      "acml_vol",
      Map.ofEntries(
          Map.entry("FID_COND_MRKT_DIV_CODE", "J"),
          Map.entry("FID_COND_SCR_DIV_CODE", "20171"),
          Map.entry("FID_INPUT_ISCD", "0000"),
          Map.entry("FID_DIV_CLS_CODE", "0"),
          Map.entry("FID_BLNG_CLS_CODE", "0"),
          Map.entry("FID_TRGT_CLS_CODE", "111111111"),
          Map.entry("FID_TRGT_EXLS_CLS_CODE", "000000"),
          Map.entry("FID_INPUT_PRICE_1", "0"),
          Map.entry("FID_INPUT_PRICE_2", "0"),
          Map.entry("FID_VOL_CNT", "0"),
          Map.entry("FID_INPUT_DATE_1", "0"))),
  UP(
      "up",
      "/uapi/domestic-stock/v1/ranking/fluctuation",
      "FHPST01700000",
      "prdy_ctrt",
      fluctuationParams("0")),
  DOWN(
      "down",
      "/uapi/domestic-stock/v1/ranking/fluctuation",
      "FHPST01700000",
      "prdy_ctrt",
      fluctuationParams("1")),
  MARKET_CAP(
      "market-cap",
      "/uapi/domestic-stock/v1/ranking/market-cap",
      "FHPST01740000",
      "stck_avls",
      Map.ofEntries(
          Map.entry("fid_cond_mrkt_div_code", "J"),
          Map.entry("fid_cond_scr_div_code", "20174"),
          Map.entry("fid_div_cls_code", "0"),
          Map.entry("fid_input_iscd", "0000"),
          Map.entry("fid_trgt_cls_code", "0"),
          Map.entry("fid_trgt_exls_cls_code", "0"),
          Map.entry("fid_input_price_1", ""),
          Map.entry("fid_input_price_2", ""),
          Map.entry("fid_vol_cnt", ""))),
  CONTRACT_STRENGTH(
      "contract-strength",
      "/uapi/domestic-stock/v1/ranking/volume-power",
      "FHPST01680000",
      "tday_rltv",
      Map.ofEntries(
          Map.entry("fid_cond_mrkt_div_code", "J"),
          Map.entry("fid_cond_scr_div_code", "20168"),
          Map.entry("fid_input_iscd", "0000"),
          Map.entry("fid_div_cls_code", "0"),
          Map.entry("fid_input_price_1", ""),
          Map.entry("fid_input_price_2", ""),
          Map.entry("fid_vol_cnt", ""),
          Map.entry("fid_trgt_exls_cls_code", "0"),
          Map.entry("fid_trgt_cls_code", "0")));

  private final String value;
  private final String path;
  private final String trId;
  private final String metricField;
  private final Map<String, String> queryParams;

  RankingType(
      String value, String path, String trId, String metricField, Map<String, String> queryParams) {
    this.value = value;
    this.path = path;
    this.trId = trId;
    this.metricField = metricField;
    this.queryParams = queryParams;
  }

  public String getValue() {
    return value;
  }

  public String getPath() {
    return path;
  }

  public String getTrId() {
    return trId;
  }

  public String getMetricField() {
    return metricField;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public String getRedisKey() {
    return "ranking:" + value;
  }

  public static Optional<RankingType> from(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    return Arrays.stream(values())
        .filter(type -> type.value.equals(value.trim().toLowerCase()))
        .findFirst();
  }

  private static Map<String, String> fluctuationParams(String sortCode) {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("fid_cond_mrkt_div_code", "J");
    params.put("fid_cond_scr_div_code", "20170");
    params.put("fid_input_iscd", "0000");
    params.put("fid_rank_sort_cls_code", sortCode);
    params.put("fid_input_cnt_1", "0");
    params.put("fid_prc_cls_code", "1");
    params.put("fid_input_price_1", "");
    params.put("fid_input_price_2", "");
    params.put("fid_vol_cnt", "");
    params.put("fid_trgt_cls_code", "0");
    params.put("fid_trgt_exls_cls_code", "0");
    params.put("fid_div_cls_code", "0");
    params.put("fid_rsfl_rate1", "");
    params.put("fid_rsfl_rate2", "");
    return params;
  }
}
