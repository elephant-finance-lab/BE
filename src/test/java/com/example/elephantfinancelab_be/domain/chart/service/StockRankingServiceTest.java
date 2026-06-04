package com.example.elephantfinancelab_be.domain.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.chart.dto.res.RankingResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.RankingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class StockRankingServiceTest {

  private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final KisStockRankingClient kisStockRankingClient = mock(KisStockRankingClient.class);
  private final StockRankingService service =
      new StockRankingService(stringRedisTemplate, objectMapper, kisStockRankingClient);

  @BeforeEach
  void setUp() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void fallsBackToKisWhenRedisReadUnavailable() {
    RankingResDTO.RankingItem item =
        new RankingResDTO.RankingItem(1, "005930", "삼성전자", 70000L, 100L, null, 1000L, null);
    when(valueOperations.get(RankingType.VOLUME.getRedisKey()))
        .thenThrow(new RedisConnectionFailureException("redis down"));
    when(kisStockRankingClient.fetchRanking(RankingType.VOLUME)).thenReturn(List.of(item));

    RankingResDTO.RankingResponse result = service.findRanking("volume");

    assertThat(result.items()).containsExactly(item);
    verify(kisStockRankingClient).fetchRanking(RankingType.VOLUME);
  }

  @Test
  void returnsEmptyRankingWhenRedisAndKisAreUnavailable() {
    when(valueOperations.get(RankingType.VOLUME.getRedisKey()))
        .thenThrow(new RedisConnectionFailureException("redis down"));
    when(kisStockRankingClient.fetchRanking(RankingType.VOLUME))
        .thenThrow(new IllegalStateException("kis unavailable"));

    RankingResDTO.RankingResponse result = service.findRanking("volume");

    assertThat(result.type()).isEqualTo("volume");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void ignoresRedisWriteFailureAfterKisFallbackSucceeds() {
    RankingResDTO.RankingItem item =
        new RankingResDTO.RankingItem(1, "005930", "삼성전자", 70000L, 100L, null, 1000L, null);
    when(valueOperations.get(RankingType.VOLUME.getRedisKey())).thenReturn(null);
    when(kisStockRankingClient.fetchRanking(RankingType.VOLUME)).thenReturn(List.of(item));
    org.mockito.Mockito.doThrow(new RedisConnectionFailureException("redis down"))
        .when(valueOperations)
        .set(anyString(), anyString(), any());

    RankingResDTO.RankingResponse result = service.findRanking("volume");

    assertThat(result.items()).containsExactly(item);
  }
}
