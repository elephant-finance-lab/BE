package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockFinancialResDTO;

public interface StockFinancialQueryService {

  StockFinancialResDTO.Financial getFinancial(String ticker, String statement, String period);
}
