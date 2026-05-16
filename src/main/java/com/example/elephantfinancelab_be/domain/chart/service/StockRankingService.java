package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.dto.res.RankingResDTO;
import com.example.elephantfinancelab_be.domain.chart.entity.RankingType;
import com.example.elephantfinancelab_be.domain.chart.exception.ChartException;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockRankingService {

  private static final Duration CACHE_TTL = Duration.ofSeconds(20);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final KisStockRankingClient kisStockRankingClient;

  public RankingResDTO.RankingResponse findRanking(String typeValue) {
    RankingType type =
        RankingType.from(typeValue)
            .orElseThrow(() -> new ChartException(ChartErrorCode.INVALID_RANKING_TYPE));

    RankingResDTO.RankingResponse cachedResponse = findCachedRanking(type);
    if (cachedResponse != null) {
      log.info("종목 랭킹 캐시 조회 성공. key={}", type.getRedisKey());
      return cachedResponse;
    }

    log.info("종목 랭킹 캐시 없음. key={}", type.getRedisKey());
    try {
      RankingResDTO.RankingResponse response =
          new RankingResDTO.RankingResponse(
              type.getValue(), kisStockRankingClient.fetchRanking(type));
      saveCache(type, response);
      return response;
    } catch (RuntimeException e) {
      log.error("한국투자증권 종목 랭킹 조회 실패로 빈 목록을 반환합니다. type={}", type.getValue(), e);
      return new RankingResDTO.RankingResponse(type.getValue(), List.of());
    }
  }

  private RankingResDTO.RankingResponse findCachedRanking(RankingType type) {
    String json = stringRedisTemplate.opsForValue().get(type.getRedisKey());
    if (json == null || json.isBlank()) {
      return null;
    }

    try {
      return objectMapper.readValue(json, RankingResDTO.RankingResponse.class);
    } catch (JsonProcessingException e) {
      log.warn(
          "code={}, message={}, key={}",
          ChartErrorCode.RANKING_CACHE_DESERIALIZE_FAILED.getCode(),
          ChartErrorCode.RANKING_CACHE_DESERIALIZE_FAILED.getMessage(),
          type.getRedisKey(),
          e);
      return null;
    }
  }

  private void saveCache(RankingType type, RankingResDTO.RankingResponse response) {
    try {
      String json = objectMapper.writeValueAsString(response);
      stringRedisTemplate.opsForValue().set(type.getRedisKey(), json, CACHE_TTL);
      log.info("종목 랭킹 캐시 저장 완료. key={}, ttlSeconds={}", type.getRedisKey(), CACHE_TTL.toSeconds());
    } catch (JsonProcessingException e) {
      throw new ChartException(ChartErrorCode.RANKING_CACHE_SERIALIZE_FAILED, e);
    }
  }
}
