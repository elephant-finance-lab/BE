package com.example.elephantfinancelab_be.domain.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class MarketIndexRedisServiceTest {

  private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2026, 5, 25, 15, 30);

  private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final MarketIndexRedisService service =
      new MarketIndexRedisService(stringRedisTemplate, objectMapper);

  @BeforeEach
  void setUp() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(stringRedisTemplate.getExpire(MarketIndexMarket.KOSPI.getRedisKey())).thenReturn(-1L);
  }

  @Test
  void saveStoresMarketIndexWithoutTtl() throws Exception {
    MarketIndexResDTO.MarketIndex index =
        new MarketIndexResDTO.MarketIndex(
            "KOSPI",
            new BigDecimal("2735.24"),
            new BigDecimal("12.31"),
            new BigDecimal("0.45"),
            TIMESTAMP);

    boolean saved = service.save(MarketIndexMarket.KOSPI, index);

    assertThat(saved).isTrue();
    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOperations).set(eq(MarketIndexMarket.KOSPI.getRedisKey()), jsonCaptor.capture());
    verify(stringRedisTemplate).persist(MarketIndexMarket.KOSPI.getRedisKey());
    verify(stringRedisTemplate).getExpire(MarketIndexMarket.KOSPI.getRedisKey());

    JsonNode savedJson = objectMapper.readTree(jsonCaptor.getValue());
    assertThat(savedJson.path("market").asText()).isEqualTo("KOSPI");
    assertThat(savedJson.path("value").decimalValue()).isEqualByComparingTo("2735.24");
    assertThat(savedJson.path("timestamp").asText()).isEqualTo("2026-05-25T15:30:00");
  }

  @Test
  void saveSkipsInvalidIndexWithoutOverwritingRedis() {
    MarketIndexResDTO.MarketIndex index =
        new MarketIndexResDTO.MarketIndex(
            "KOSPI", null, new BigDecimal("12.31"), new BigDecimal("0.45"), TIMESTAMP);

    boolean saved = service.save(MarketIndexMarket.KOSPI, index);

    assertThat(saved).isFalse();
    verify(valueOperations, never()).set(anyString(), anyString());
    verify(stringRedisTemplate, never()).persist(anyString());
  }

  @Test
  void findLatestMarketIndexesReturnsNullOnlyWhenRedisMisses() throws Exception {
    MarketIndexResDTO.MarketIndex kospi =
        new MarketIndexResDTO.MarketIndex(
            "KOSPI",
            new BigDecimal("2735.24"),
            new BigDecimal("12.31"),
            new BigDecimal("0.45"),
            TIMESTAMP);
    when(valueOperations.get(MarketIndexMarket.KOSPI.getRedisKey()))
        .thenReturn(objectMapper.writeValueAsString(kospi));
    when(valueOperations.get(MarketIndexMarket.KOSDAQ.getRedisKey())).thenReturn(null);

    MarketIndexResDTO.MarketIndexes result = service.findLatestMarketIndexes();

    assertThat(result.kospi()).isEqualTo(kospi);
    assertThat(result.kosdaq()).isNull();
  }
}
