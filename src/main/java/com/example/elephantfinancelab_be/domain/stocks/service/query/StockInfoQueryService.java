package com.example.elephantfinancelab_be.domain.stocks.service.query;

import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockInfoResDTO;

public interface StockInfoQueryService {

  // 변경내용: Mono 제거, 동기 반환으로 변경
  StockInfoResDTO.Info getInfo(String ticker, String period);
}
