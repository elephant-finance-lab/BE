package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartRange;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockChartType;
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
public class StockChartRedisService {

  private static final String CHART_KEY_PREFIX = "stock:chart:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(StockChartResDTO.Chart chart) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(
              redisKey(chart.ticker(), StockChartRange.from(chart.range()), chart.type()),
              objectMapper.writeValueAsString(chart),
              StockChartRange.from(chart.range()).getCacheTtl());
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_CHART_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public StockChartResDTO.Chart find(String ticker, StockChartRange range, StockChartType type) {
    String json = stringRedisTemplate.opsForValue().get(redisKey(ticker, range, type));
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, StockChartResDTO.Chart.class);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_CHART_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  public boolean exists(String ticker, StockChartRange range, StockChartType type) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey(ticker, range, type)));
  }

  private String redisKey(String ticker, StockChartRange range, StockChartType type) {
    return CHART_KEY_PREFIX
        + ticker.trim().toUpperCase(Locale.ROOT)
        + ":"
        + range.getValue()
        + ":"
        + type.name();
  }
}
