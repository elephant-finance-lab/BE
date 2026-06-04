package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockInfoPriceRedisService {

  private static final String PRICE_KEY_PREFIX = "stock:info:price:v2:";
  private static final Duration PRICE_CACHE_TTL = Duration.ofSeconds(30);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(String ticker, StockInfoResDTO.Price price) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(redisKey(ticker), objectMapper.writeValueAsString(price), PRICE_CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_INFO_PRICE_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public StockInfoResDTO.Price find(String ticker) {
    String json = stringRedisTemplate.opsForValue().get(redisKey(ticker));
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, StockInfoResDTO.Price.class);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_INFO_PRICE_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  private String redisKey(String ticker) {
    return PRICE_KEY_PREFIX + ticker.trim().toUpperCase(Locale.ROOT);
  }
}
