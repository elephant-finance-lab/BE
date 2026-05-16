package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.converter.StockConverter;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPricePushService {

  private final StockRepository stockRepository;
  private final StockSummaryRedisService stockSummaryRedisService;
  private final StockChartRealtimeService stockChartRealtimeService;
  private final SimpMessagingTemplate messagingTemplate;

  public void updateAndPush(StockPriceRealtimeParser.ParsedStockPrice parsed) {
    String ticker = normalizeTicker(parsed.ticker());
    if (ticker == null) {
      log.debug("종목코드가 없는 실시간 체결가를 건너뜁니다.");
      return;
    }

    stockRepository
        .findByTicker(ticker)
        .ifPresentOrElse(
            stock -> updateAndPush(stock, parsed),
            () -> log.debug("DB에 없는 종목 실시간 체결가를 건너뜁니다. ticker={}", ticker));
  }

  private void updateAndPush(Stock stock, StockPriceRealtimeParser.ParsedStockPrice parsed) {
    StockResDTO.Summary summary =
        StockConverter.toSummary(
            stock,
            parsed.currentPriceKrw(),
            parsed.changeAmountKrw(),
            parsed.changeRate(),
            parsed.signCode(),
            parsed.updatedAt());

    try {
      stockSummaryRedisService.save(summary);
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_SUMMARY_CACHE_SAVE_FAILED.getCode(),
          StockErrorCode.STOCK_SUMMARY_CACHE_SAVE_FAILED.getMessage(),
          stock.getTicker(),
          e);
    }

    try {
      messagingTemplate.convertAndSend(destination(stock.getTicker()), summary);
      log.debug("종목 실시간 체결가 push 완료. ticker={}", stock.getTicker());
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.STOCK_PRICE_PUSH_FAILED.getCode(),
          StockErrorCode.STOCK_PRICE_PUSH_FAILED.getMessage(),
          stock.getTicker(),
          e);
    }

    stockChartRealtimeService.updateAndPush(stock.getTicker(), parsed);
  }

  private String destination(String ticker) {
    return "/topic/stocks/" + ticker.trim().toUpperCase(Locale.ROOT) + "/price";
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      return null;
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
