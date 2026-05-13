package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.stocks.converter.StockConverter;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
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
  private final SimpMessagingTemplate messagingTemplate;

  public void updateAndPush(StockPriceRealtimeParser.ParsedStockPrice parsed) {
    stockRepository
        .findByTickerIgnoreCase(parsed.ticker())
        .ifPresentOrElse(
            stock -> updateAndPush(stock, parsed),
            () -> log.debug("DB에 없는 종목 실시간 체결가를 건너뜁니다. ticker={}", parsed.ticker()));
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

    stockSummaryRedisService.save(summary);
    messagingTemplate.convertAndSend(destination(stock.getTicker()), summary);
    log.debug("종목 실시간 체결가 push 완료. ticker={}", stock.getTicker());
  }

  private String destination(String ticker) {
    return "/topic/stocks/" + ticker.trim().toUpperCase() + "/price";
  }
}
