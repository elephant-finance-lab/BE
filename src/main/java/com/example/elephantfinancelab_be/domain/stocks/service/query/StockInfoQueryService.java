package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;
import reactor.core.publisher.Mono;

public interface StockInfoQueryService {

  Mono<StockInfoResDTO.Info> getInfo(String ticker, String period);
}
