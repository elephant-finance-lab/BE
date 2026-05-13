package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialPeriod;
import com.example.elephantfinancelab_be.domain.stocks.entity.StockFinancialStatement;
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
public class StockFinancialRedisService {

  private static final String FINANCIAL_KEY_PREFIX = "stock:financial:";
  private static final Duration FINANCIAL_CACHE_TTL = Duration.ofHours(1);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void save(StockFinancialResDTO.Financial financial) {
    try {
      stringRedisTemplate
          .opsForValue()
          .set(
              redisKey(financial.ticker(), financial.statement(), financial.period()),
              objectMapper.writeValueAsString(financial),
              FINANCIAL_CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_FINANCIAL_CACHE_SERIALIZE_FAILED, e);
    }
  }

  public StockFinancialResDTO.Financial find(
      String ticker, StockFinancialStatement statement, StockFinancialPeriod period) {
    String json = stringRedisTemplate.opsForValue().get(redisKey(ticker, statement, period));
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, StockFinancialResDTO.Financial.class);
    } catch (JsonProcessingException e) {
      throw new StockException(StockErrorCode.STOCK_FINANCIAL_CACHE_DESERIALIZE_FAILED, e);
    }
  }

  private String redisKey(
      String ticker, StockFinancialStatement statement, StockFinancialPeriod period) {
    return FINANCIAL_KEY_PREFIX
        + ticker.trim().toUpperCase(Locale.ROOT)
        + ":"
        + statement.name()
        + ":"
        + period.name();
  }
}
