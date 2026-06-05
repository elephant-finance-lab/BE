package com.example.elephantfinancelab_be.domain.stocks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockRegistrationServiceTest {

  private final StockRepository stockRepository = mock(StockRepository.class);
  private final StockRegistrationService service = new StockRegistrationService(stockRepository);

  @Test
  void updatesStoredNameWhenNameEchoesTickerAndResolvedNameIsAvailable() {
    Stock stock = Stock.builder().ticker("005930").name("005930").build();
    when(stockRepository.findByTicker("005930")).thenReturn(Optional.of(stock));

    service.updateNameIfTickerEcho("005930", "삼성전자");

    assertThat(stock.getName()).isEqualTo("삼성전자");
    verify(stockRepository).saveAndFlush(stock);
  }

  @Test
  void keepsStoredNameWhenResolvedNameStillEchoesTicker() {
    Stock stock = Stock.builder().ticker("005930").name("005930").build();

    service.updateNameIfTickerEcho("005930", "005930");

    assertThat(stock.getName()).isEqualTo("005930");
    verify(stockRepository, never()).saveAndFlush(stock);
  }
}
