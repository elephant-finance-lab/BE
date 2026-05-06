package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketIndexRedisService {

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(MarketIndexMarket market, MarketIndexResDTO.MarketIndex index) {
    try {
      String json = objectMapper.writeValueAsString(index);
      stringRedisTemplate.opsForValue().set(market.getRedisKey(), json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize market index", e);
    }
  }

  public MarketIndexResDTO.MarketIndexes findLatestMarketIndexes() {
    return new MarketIndexResDTO.MarketIndexes(
        findLatest(MarketIndexMarket.KOSPI), findLatest(MarketIndexMarket.KOSDAQ));
  }

  private MarketIndexResDTO.MarketIndex findLatest(MarketIndexMarket market) {
    String json = stringRedisTemplate.opsForValue().get(market.getRedisKey());
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, MarketIndexResDTO.MarketIndex.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize market index", e);
    }
  }
}
