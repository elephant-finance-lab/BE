package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceClient;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceWebSocketClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSummaryRedisService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryServiceImpl implements StockQueryService {

  private final StockRepository stockRepository;
  private final StockSummaryRedisService stockSummaryRedisService;
  private final KisStockPriceClient kisStockPriceClient;
  private final KisStockPriceWebSocketClient kisStockPriceWebSocketClient;

  @Override
  public StockResDTO.Summary getSummary(String ticker) {
    Stock stock =
        stockRepository
            .findByTicker(normalizeTicker(ticker))
            .orElseThrow(() -> new StockException(StockErrorCode.STOCK_NOT_FOUND));

    StockResDTO.Summary cachedSummary = stockSummaryRedisService.find(stock.getTicker());
    if (cachedSummary != null) {
      kisStockPriceWebSocketClient.subscribe(stock.getTicker());
      log.info("종목 요약 캐시 조회 성공. ticker={}", stock.getTicker());
      return cachedSummary;
    }

    StockResDTO.Summary summary = kisStockPriceClient.fetchSummary(stock);
    stockSummaryRedisService.save(summary);
    kisStockPriceWebSocketClient.subscribe(stock.getTicker());
    return summary;
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      throw new StockException(StockErrorCode.STOCK_NOT_FOUND);
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
  }
}
