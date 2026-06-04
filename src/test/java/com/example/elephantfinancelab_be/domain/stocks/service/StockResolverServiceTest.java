package com.example.elephantfinancelab_be.domain.stocks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.exception.StockException;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockResolverServiceTest {

  private final StockRepository stockRepository = mock(StockRepository.class);
  private final KisStockBasicInfoClient kisStockBasicInfoClient =
      mock(KisStockBasicInfoClient.class);
  private final StockRegistrationService stockRegistrationService =
      mock(StockRegistrationService.class);
  private final StockResolverService service =
      new StockResolverService(stockRepository, kisStockBasicInfoClient, stockRegistrationService);

  @Test
  void returnsStoredStockWithoutCallingExternalApi() {
    Stock stock = Stock.builder().ticker("005930").name("삼성전자").build();
    when(stockRepository.findByTicker("005930")).thenReturn(Optional.of(stock));

    Stock result = service.resolve("005930");

    assertThat(result).isSameAs(stock);
    verifyNoInteractions(kisStockBasicInfoClient, stockRegistrationService);
  }

  @Test
  void fetchesAndRegistersStockWhenTickerDoesNotExistInDatabase() {
    Stock stock = Stock.builder().ticker("203650").name("드림시큐리티").build();
    when(stockRepository.findByTicker("203650")).thenReturn(Optional.empty());
    when(kisStockBasicInfoClient.fetchStockName("203650")).thenReturn("드림시큐리티");
    when(stockRegistrationService.saveIfAbsent("203650", "드림시큐리티")).thenReturn(stock);

    Stock result = service.resolve("203650");

    assertThat(result).isSameAs(stock);
    verify(kisStockBasicInfoClient).fetchStockName("203650");
    verify(stockRegistrationService).saveIfAbsent("203650", "드림시큐리티");
  }

  @Test
  void registersTickerFallbackWhenVirtualBasicInfoApiIsUnavailable() {
    Stock stock = Stock.builder().ticker("006400").name("006400").build();
    when(stockRepository.findByTicker("006400")).thenReturn(Optional.empty());
    when(kisStockBasicInfoClient.fetchStockName("006400"))
        .thenThrow(new StockException(StockErrorCode.KIS_STOCK_BASIC_INFO_API_FAILED));
    when(stockRegistrationService.saveIfAbsent("006400", "006400")).thenReturn(stock);

    Stock result = service.resolve("006400");

    assertThat(result).isSameAs(stock);
    verify(kisStockBasicInfoClient).fetchStockName("006400");
    verify(stockRegistrationService).saveIfAbsent("006400", "006400");
  }
}
