package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockDailyPriceResDTO;
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
public class StockDailyPriceRedisService {

  private static final String DAILY_PRICE_KEY_PREFIX = "stock:daily:";
  private static final Duration DAILY_PRICE_CACHE_TTL = Duration.ofSeconds(30);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(StockDailyPriceResDTO.DailyPrices dailyPrices) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(
              redisKey(dailyPrices.ticker()),
              objectMapper.writeValueAsString(dailyPrices),
              DAILY_PRICE_CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_DAILY_PRICE_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public StockDailyPriceResDTO.DailyPrices find(String ticker) {
    String json = stringRedisTemplate.opsForValue().get(redisKey(ticker));
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, StockDailyPriceResDTO.DailyPrices.class);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_DAILY_PRICE_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  private String redisKey(String ticker) {
    return DAILY_PRICE_KEY_PREFIX + ticker.trim().toUpperCase(Locale.ROOT);
  }
}
