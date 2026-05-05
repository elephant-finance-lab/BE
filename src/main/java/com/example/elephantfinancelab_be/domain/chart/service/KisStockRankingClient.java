package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.RankingResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.RankingType;
import com.example.elephantfinancelab_be.domain.chart.exception.ChartException;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisStockRankingClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final KisAccessTokenClient accessTokenClient;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();

  public List<RankingResDTO.RankingItem> fetchRanking(RankingType type) {
    try {
      log.info("한국투자증권 종목 랭킹 API 호출. type={}, trId={}", type.getValue(), type.getTrId());
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(rankingUri(type))
              .timeout(REQUEST_TIMEOUT)
              .header("content-type", MediaType.APPLICATION_JSON_VALUE + "; charset=utf-8")
              .header("authorization", "Bearer " + accessTokenClient.getAccessToken())
              .header("appkey", kisProperties.getAppKey())
              .header("appsecret", kisProperties.getAppSecret())
              .header("tr_id", type.getTrId())
              .header("custtype", "P")
              .GET()
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn(
            "{} status={}",
            ChartErrorCode.KIS_RANKING_API_FAILED.getMessage(),
            response.statusCode());
        throw new ChartException(ChartErrorCode.KIS_RANKING_API_FAILED);
      }

      JsonNode root = objectMapper.readTree(response.body());
      if (!"0".equals(root.path("rt_cd").asText())) {
        log.warn(
            "{} msg_cd={}, msg={}",
            ChartErrorCode.KIS_RANKING_API_FAILED.getMessage(),
            root.path("msg_cd").asText(),
            root.path("msg1").asText());
        throw new ChartException(ChartErrorCode.KIS_RANKING_API_FAILED);
      }

      return toRankingItems(type, root.path("output"));
    } catch (IOException e) {
      throw new ChartException(ChartErrorCode.KIS_RANKING_API_FAILED, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ChartException(ChartErrorCode.KIS_RANKING_API_FAILED, e);
    }
  }

  private List<RankingResDTO.RankingItem> toRankingItems(RankingType type, JsonNode output) {
    List<RankingResDTO.RankingItem> items = new ArrayList<>();
    if (!output.isArray()) {
      return items;
    }

    int fallbackRank = 1;
    for (JsonNode node : output) {
      items.add(
          new RankingResDTO.RankingItem(
              intValue(node, "data_rank", fallbackRank),
              firstText(node, "mksc_shrn_iscd", "stck_shrn_iscd"),
              textValue(node, "hts_kor_isnm"),
              longValue(node, "stck_prpr"),
              longValue(node, "prdy_vrss"),
              decimalValue(node, "prdy_ctrt"),
              longValue(node, "acml_vol"),
              decimalValue(node, type.getMetricField())));
      fallbackRank++;
    }

    return items;
  }

  private URI rankingUri(RankingType type) {
    String baseUrl = kisProperties.getBaseUrl();
    String normalizedBaseUrl =
        baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return URI.create(
        normalizedBaseUrl + type.getPath() + "?" + queryString(type.getQueryParams()));
  }

  private String queryString(Map<String, String> params) {
    StringJoiner joiner = new StringJoiner("&");
    params.forEach(
        (key, value) -> joiner.add(encode(key) + "=" + encode(value == null ? "" : value)));
    return joiner.toString();
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String firstText(JsonNode node, String firstField, String secondField) {
    String firstValue = textValue(node, firstField);
    if (firstValue != null && !firstValue.isBlank()) {
      return firstValue;
    }
    return textValue(node, secondField);
  }

  private String textValue(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText();
    return value.isBlank() ? null : value;
  }

  private Integer intValue(JsonNode node, String fieldName, int defaultValue) {
    String value = textValue(node, fieldName);
    return value == null ? defaultValue : Integer.valueOf(value);
  }

  private Long longValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    return value == null ? null : Long.valueOf(value);
  }

  private BigDecimal decimalValue(JsonNode node, String fieldName) {
    String value = textValue(node, fieldName);
    return value == null ? null : new BigDecimal(value);
  }
}
