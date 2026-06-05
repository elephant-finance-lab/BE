package com.example.elephantfinancelab_be.domain.stocks.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceClient;
import com.example.elephantfinancelab_be.domain.stocks.service.KisStockPriceWebSocketClient;
import com.example.elephantfinancelab_be.domain.stocks.service.StockRegistrationService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockResolverService;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSummaryRedisService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StockQueryServiceImplTest {

  private final Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
  private final StockResolverService stockResolverService = mock(StockResolverService.class);
  private final StockRegistrationService stockRegistrationService =
      mock(StockRegistrationService.class);
  private final StockSummaryRedisService stockSummaryRedisService =
      mock(StockSummaryRedisService.class);
  private final KisStockPriceClient kisStockPriceClient = mock(KisStockPriceClient.class);
  private final KisStockPriceWebSocketClient kisStockPriceWebSocketClient =
      mock(KisStockPriceWebSocketClient.class);
  private final StockQueryServiceImpl service =
      new StockQueryServiceImpl(
          stockResolverService,
          stockRegistrationService,
          stockSummaryRedisService,
          kisStockPriceClient,
          kisStockPriceWebSocketClient);

  @Test
  void returnsCachedSummaryWithoutCallingKis() {
    StockResDTO.Summary cached = summary(72000L);
    when(stockSummaryRedisService.find("005930")).thenReturn(cached);

    StockResDTO.Summary result = service.getSummary("005930");

    assertThat(result).isSameAs(cached);
    verify(kisStockPriceClient, never()).fetchSummary(stock);
    verify(stockRegistrationService).updateNameIfTickerEcho("005930", "삼성전자");
    verify(kisStockPriceWebSocketClient).subscribe("005930");
  }

  @Test
  void refreshesCachedSummaryWhenStockNameEchoesTicker() {
    StockResDTO.Summary cached = summary("005930", 72000L);
    StockResDTO.Summary fetched = summary("삼성전자", 72500L);
    when(stockSummaryRedisService.find("005930")).thenReturn(cached);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock)).thenReturn(fetched);

    StockResDTO.Summary result = service.getSummary("005930");

    assertThat(result).isSameAs(fetched);
    verify(kisStockPriceClient, times(1)).fetchSummary(stock);
    verify(stockRegistrationService).updateNameIfTickerEcho("005930", "삼성전자");
    verify(stockSummaryRedisService).save(fetched);
    verify(kisStockPriceWebSocketClient).subscribe("005930");
  }

  @Test
  void fetchesSummaryAndCachesWhenFirstKisAttemptSucceeds() {
    StockResDTO.Summary fetched = summary(72500L);
    when(stockSummaryRedisService.find("005930")).thenReturn(null);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock)).thenReturn(fetched);

    StockResDTO.Summary result = service.getSummary("005930");

    assertThat(result).isSameAs(fetched);
    verify(kisStockPriceClient, times(1)).fetchSummary(stock);
    verify(stockSummaryRedisService).save(fetched);
    verify(kisStockPriceWebSocketClient).subscribe("005930");
  }

  @Test
  void retriesTransientKisSummaryFailureAndCachesSuccess() {
    StockResDTO.Summary fetched = summary(72500L);
    when(stockSummaryRedisService.find("005930")).thenReturn(null);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock))
        .thenThrow(new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED))
        .thenReturn(fetched);

    StockResDTO.Summary result = service.getSummary("005930");

    assertThat(result).isSameAs(fetched);
    verify(kisStockPriceClient, times(2)).fetchSummary(stock);
    verify(stockSummaryRedisService).save(fetched);
    verify(kisStockPriceWebSocketClient).subscribe("005930");
  }

  @Test
  void succeedsAfterTwoRetriesForTransientKisSummaryFailure() {
    StockResDTO.Summary fetched = summary(72800L);
    when(stockSummaryRedisService.find("005930")).thenReturn(null);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock))
        .thenThrow(new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED))
        .thenThrow(new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED))
        .thenReturn(fetched);

    StockResDTO.Summary result = service.getSummary("005930");

    assertThat(result).isSameAs(fetched);
    verify(kisStockPriceClient, times(3)).fetchSummary(stock);
    verify(stockSummaryRedisService).save(fetched);
    verify(kisStockPriceWebSocketClient).subscribe("005930");
  }

  @Test
  void propagatesLastExceptionWhenAllTransientKisSummaryAttemptsFail() {
    StockException first = new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED);
    StockException second = new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED);
    StockException last = new StockException(StockErrorCode.KIS_STOCK_PRICE_API_FAILED);
    when(stockSummaryRedisService.find("005930")).thenReturn(null);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock)).thenThrow(first, second, last);

    assertThatThrownBy(() -> service.getSummary("005930")).isSameAs(last);

    verify(kisStockPriceClient, times(3)).fetchSummary(stock);
    verify(stockSummaryRedisService, never()).save(org.mockito.Mockito.any());
    verify(kisStockPriceWebSocketClient, never()).subscribe("005930");
  }

  @Test
  void propagatesNonRetryableKisSummaryFailureWithoutRetry() {
    StockException exception =
        new StockException(StockErrorCode.KIS_STOCK_PRICE_RESPONSE_PARSE_FAILED);
    when(stockSummaryRedisService.find("005930")).thenReturn(null);
    when(stockResolverService.resolve("005930")).thenReturn(stock);
    when(kisStockPriceClient.fetchSummary(stock)).thenThrow(exception);

    assertThatThrownBy(() -> service.getSummary("005930")).isSameAs(exception);

    verify(kisStockPriceClient, times(1)).fetchSummary(stock);
    verify(stockSummaryRedisService, never()).save(org.mockito.Mockito.any());
    verify(kisStockPriceWebSocketClient, never()).subscribe("005930");
  }

  private StockResDTO.Summary summary(long price) {
    return summary("삼성전자", price);
  }

  private StockResDTO.Summary summary(String stockName, long price) {
    return StockResDTO.Summary.builder()
        .stockName(stockName)
        .ticker("005930")
        .currentPriceKrw(price)
        .changeAmountKrw(100L)
        .changeRate(BigDecimal.valueOf(0.14))
        .signCode("2")
        .updatedAt(LocalDateTime.parse("2026-06-04T09:30:00"))
        .build();
  }
}
