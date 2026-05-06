package com.example.elephantfinancelab_be.domain.chart.controller;

import com.example.elephantfinancelab_be.domain.chart.dto.res.MarketIndexResDTO;
import com.example.elephantfinancelab_be.domain.chart.dto.res.RankingResDTO;
import com.example.elephantfinancelab_be.domain.chart.service.MarketIndexRedisService;
import com.example.elephantfinancelab_be.domain.chart.service.StockRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chart", description = "차트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chart")
public class ChartController {

  private final MarketIndexRedisService marketIndexRedisService;
  private final StockRankingService stockRankingService;

  @Operation(summary = "최신 시장 지수 조회", description = "Redis에 저장된 KOSPI/KOSDAQ 최신 지수를 조회합니다.")
  @GetMapping("/market")
  public ResponseEntity<MarketIndexResDTO.MarketIndexes> getMarketIndexes() {
    return ResponseEntity.ok(marketIndexRedisService.findLatestMarketIndexes());
  }

  @Operation(summary = "종목 랭킹 조회", description = "type별 국내주식 종목 랭킹을 조회합니다.")
  @GetMapping("/ranking")
  public ResponseEntity<RankingResDTO.RankingResponse> getRanking(
      @Parameter(description = "volume, up, down, market-cap, contract-strength") @RequestParam
          String type) {
    return ResponseEntity.ok(stockRankingService.findRanking(type));
  }
}
