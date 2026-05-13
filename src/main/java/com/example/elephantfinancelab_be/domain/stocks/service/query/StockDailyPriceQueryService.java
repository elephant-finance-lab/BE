package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockDailyPriceResDTO;

public interface StockDailyPriceQueryService {

  StockDailyPriceResDTO.DailyPrices getDailyPrices(String ticker);
}
