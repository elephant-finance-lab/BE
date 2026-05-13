package com.example.elephantfinancelab_be.domain.stocks.controller;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.service.query.StockChartQueryService;
import com.example.elephantfinancelab_be.domain.stocks.service.query.StockQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stocks", description = "종목 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

  private final StockQueryService stockQueryService;
  private final StockChartQueryService stockChartQueryService;

  @Operation(summary = "종목 상세 상단 조회", description = "국내주식 종목명, 현재가, 전일 대비 정보를 조회합니다.")
  @GetMapping("/{ticker}/summary")
  public ResponseEntity<ApiResponse<StockResDTO.Summary>> getSummary(
      @Parameter(description = "종목코드. 예: 005930") @PathVariable String ticker) {
    StockResDTO.Summary result = stockQueryService.getSummary(ticker);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "종목 차트 시계열 조회", description = "라인/캔들 차트 초기 데이터를 조회합니다.")
  @GetMapping("/{ticker}/chart")
  public ResponseEntity<ApiResponse<StockChartResDTO.Chart>> getChart(
      @Parameter(description = "종목코드. 예: 005930") @PathVariable String ticker,
      @Parameter(description = "1D, 1W, 3M, 1Y, 5Y, ALL") @RequestParam String range,
      @Parameter(description = "LINE, CANDLE") @RequestParam String type) {
    StockChartResDTO.Chart result = stockChartQueryService.getChart(ticker, range, type);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
