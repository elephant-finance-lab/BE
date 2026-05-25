package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.example.elephantfinancelab_be.domain.chart.exception.ChartException;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexRedisService {

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public boolean save(MarketIndexMarket market, MarketIndexResDTO.MarketIndex index) {
    String skipReason = invalidReason(market, index);
    if (skipReason != null) {
      String key = market == null ? "unknown" : market.getRedisKey();
      log.warn(
          "market index redis save skipped: key={}, reason={}, ttlPolicy=none", key, skipReason);
      return false;
    }

    try {
      String json = objectMapper.writeValueAsString(index);
      stringRedisTemplate.opsForValue().set(market.getRedisKey(), json);
      log.debug(
          "market index saved: key={}, value={}, timestamp={}, ttlPolicy=none",
          market.getRedisKey(),
          index.value(),
          index.timestamp());
      return true;
    } catch (JsonProcessingException e) {
      throw new ChartException(ChartErrorCode.MARKET_INDEX_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public MarketIndexResDTO.MarketIndexes findLatestMarketIndexes() {
    return new MarketIndexResDTO.MarketIndexes(
        findLatest(MarketIndexMarket.KOSPI), findLatest(MarketIndexMarket.KOSDAQ));
  }

  private MarketIndexResDTO.MarketIndex findLatest(MarketIndexMarket market) {
    String json = stringRedisTemplate.opsForValue().get(market.getRedisKey());
    if (json == null || json.isBlank()) {
      log.info("market index redis miss: key={}", market.getRedisKey());
      return null;
    }

    try {
      MarketIndexResDTO.MarketIndex index =
          objectMapper.readValue(json, MarketIndexResDTO.MarketIndex.class);
      log.info(
          "market index redis hit: key={}, value={}, timestamp={}",
          market.getRedisKey(),
          index.value(),
          index.timestamp());
      return index;
    } catch (JsonProcessingException e) {
      log.warn(
          "market index redis deserialize failed: key={}, keep cache value unavailable",
          market.getRedisKey(),
          e);
      throw new ChartException(ChartErrorCode.MARKET_INDEX_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  private String invalidReason(MarketIndexMarket market, MarketIndexResDTO.MarketIndex index) {
    if (market == null) {
      return "null-market";
    }
    if (index == null) {
      return "null-index";
    }
    if (!market.name().equals(index.market())) {
      return "market-mismatch";
    }
    if (isNullOrNonPositive(index.value())) {
      return "invalid-value";
    }
    if (index.change() == null) {
      return "null-change";
    }
    if (index.changeRate() == null) {
      return "null-change-rate";
    }
    if (index.timestamp() == null) {
      return "null-timestamp";
    }
    return null;
  }

  private boolean isNullOrNonPositive(BigDecimal value) {
    return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
  }
}
