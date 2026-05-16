package com.example.elephantfinancelab_be.domain.portfolio.service.query;

import com.example.elephantfinancelab_be.domain.portfolio.converter.PortfolioConverter;
import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.Position;
import com.example.elephantfinancelab_be.domain.portfolio.exception.PortfolioException;
import com.example.elephantfinancelab_be.domain.portfolio.exception.code.PortfolioErrorCode;
import com.example.elephantfinancelab_be.domain.portfolio.repository.PositionRepository;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import com.example.elephantfinancelab_be.domain.stocks.service.StockSummaryRedisService;
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
  private final StockSummaryRedisService stockSummaryRedisService;

  @Override
  public PortfolioResDTO.Summary findSummary(Long userId) {
    List<Position> positions = positionRepository.findAllByUserId(userId);
    if (positions.isEmpty()) {
      throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }
    List<PortfolioResDTO.PositionDetail> details = toDetails(positions);
    return PortfolioConverter.toSummary(details);
  }

  @Override
  public PortfolioResDTO.PositionPage findPositions(Long userId, Pageable pageable) {
    if (!positionRepository.existsByUserId(userId)) {
      throw new PortfolioException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }
    Page<Position> positionPage = positionRepository.findAllByUserId(userId, pageable);
    List<PortfolioResDTO.PositionDetail> details = toDetails(positionPage.getContent());
    Page<PortfolioResDTO.PositionDetail> detailPage =
        new PageImpl<>(details, pageable, positionPage.getTotalElements());
    return PortfolioConverter.toPositionPage(detailPage);
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
