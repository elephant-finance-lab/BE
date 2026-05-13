package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;

public interface StockQueryService {

  StockResDTO.Summary getSummary(String ticker);
}
