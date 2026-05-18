package com.example.elephantfinancelab_be.domain.portfolio.controller;

import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import com.example.elephantfinancelab_be.domain.portfolio.service.query.PortfolioQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Tag(name = "Portfolio", description = "포트폴리오 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
public class PortfolioController {

  private final PortfolioQueryService portfolioQueryService;
  private final UserRepository userRepository;

  private Long resolveUserId(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }

  @Operation(
      summary = "투자 자산 요약 조회",
      description = "사용자의 총 투자 자산과 보유 종목별 평가 금액, 수익률 및 자산 비중을 조회합니다.")
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<PortfolioResDTO.Summary>> getSummary(
      @AuthenticationPrincipal String email) {
    PortfolioResDTO.Summary result = portfolioQueryService.findSummary(resolveUserId(email));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "보유 종목 목록 조회", description = "로그인한 사용자의 전체 보유 종목 목록을 조회합니다.")
  @GetMapping("/positions")
  public ResponseEntity<ApiResponse<PortfolioResDTO.PositionPage>> getPositions(
      @AuthenticationPrincipal String email,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PortfolioResDTO.PositionPage result =
        portfolioQueryService.findPositions(resolveUserId(email), PageRequest.of(page, size));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "거래 기록 조회", description = "사용자의 매수 및 매도 거래 기록을 조회합니다.")
  @GetMapping("/trades")
  public ResponseEntity<ApiResponse<PortfolioResDTO.TradePage>> getTrades(
      @AuthenticationPrincipal String email,
      @RequestParam(required = false) TradeType type,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PortfolioResDTO.TradePage result =
        portfolioQueryService.findTrades(resolveUserId(email), type, PageRequest.of(page, size));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }
}
