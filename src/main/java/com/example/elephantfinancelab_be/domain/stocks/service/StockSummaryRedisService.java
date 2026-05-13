package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockSummaryRedisService {

  private static final String SUMMARY_KEY_PREFIX = "stock:summary:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(StockResDTO.Summary summary) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(redisKey(summary.getTicker()), objectMapper.writeValueAsString(summary));
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_SUMMARY_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public StockResDTO.Summary find(String ticker) {
    String json = stringRedisTemplate.opsForValue().get(redisKey(ticker));
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, StockResDTO.Summary.class);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_SUMMARY_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  private String redisKey(String ticker) {
    return SUMMARY_KEY_PREFIX + ticker.trim().toUpperCase(Locale.ROOT);
  }
}
