package com.example.elephantfinancelab_be.domain.stocks.controller;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stocks", description = "종목 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

  private final StockQueryService stockQueryService;

  @Operation(summary = "종목 상세 상단 조회", description = "국내주식 종목명, 현재가, 전일 대비 정보를 조회합니다.")
  @GetMapping("/{ticker}/summary")
  public ResponseEntity<ApiResponse<StockResDTO.Summary>> getSummary(
      @Parameter(description = "종목코드. 예: 005930") @PathVariable String ticker) {
    StockResDTO.Summary result = stockQueryService.getSummary(ticker);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
