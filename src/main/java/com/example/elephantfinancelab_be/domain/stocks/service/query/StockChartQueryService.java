package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockChartResDTO;

public interface StockChartQueryService {

  StockChartResDTO.Chart getChart(String ticker, String range, String type);
}
