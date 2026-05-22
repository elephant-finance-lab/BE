package com.example.elephantfinancelab_be.domain.portfolio.service.query;

import com.example.elephantfinancelab_be.domain.portfolio.converter.PortfolioConverter;
import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.Position;
import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import com.example.elephantfinancelab_be.domain.portfolio.repository.PositionRepository;
import com.example.elephantfinancelab_be.domain.portfolio.repository.TradeRepository;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSummaryRedisService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryServiceImpl implements PortfolioQueryService {

  private final PositionRepository positionRepository;
  private final TradeRepository tradeRepository;
  private final StockSummaryRedisService stockSummaryRedisService;

  @Override
  public PortfolioResDTO.Summary findSummary(Long userId) {
    List<Position> positions = positionRepository.findAllByUserId(userId);
    if (positions.isEmpty()) {
      return PortfolioResDTO.Summary.builder()
          .totalAsset(0L)
          .totalProfit(0L)
          .totalProfitRate(0.0)
          .positions(Collections.emptyList())
          .build();
    }
    List<PortfolioResDTO.PositionDetail> details = toDetails(positions);
    return PortfolioConverter.toSummary(details);
  }

  @Override
  public PortfolioResDTO.PositionPage findPositions(Long userId, Pageable pageable) {
    Page<Position> positionPage = positionRepository.findAllByUserId(userId, pageable);
    if (positionPage.isEmpty()) {
      return PortfolioResDTO.PositionPage.builder()
          .page(pageable.getPageNumber())
          .size(pageable.getPageSize())
          .totalElements(0L)
          .totalPages(0)
          .hasNext(false)
          .positions(Collections.emptyList())
          .build();
    }
    List<PortfolioResDTO.PositionDetail> details = toDetails(positionPage.getContent());
    Page<PortfolioResDTO.PositionDetail> detailPage =
        new PageImpl<>(details, pageable, positionPage.getTotalElements());
    return PortfolioConverter.toPositionPage(detailPage);
  }

  @Override
  public PortfolioResDTO.TradePage findTrades(Long userId, TradeType type, Pageable pageable) {
    Page<com.example.elephantfinancelab_be.domain.portfolio.entity.Trade> tradePage =
        type != null
            ? tradeRepository.findAllByUserIdAndType(userId, type, pageable)
            : tradeRepository.findAllByUserId(userId, pageable);
    return PortfolioConverter.toTradePage(tradePage);
  }

  private List<PortfolioResDTO.PositionDetail> toDetails(List<Position> positions) {
    return positions.stream()
        .map(
            p -> {
              StockResDTO.Summary summary = stockSummaryRedisService.find(p.getTickerCode());
              return PortfolioConverter.toPositionDetail(p, summary);
            })
        .toList();
  }
}
