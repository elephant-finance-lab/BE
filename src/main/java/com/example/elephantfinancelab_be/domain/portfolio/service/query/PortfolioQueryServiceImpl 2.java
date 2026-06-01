package com.example.elephantfinancelab_be.domain.portfolio.service.query;

import com.example.elephantfinancelab_be.domain.portfolio.converter.PortfolioConverter;
import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.HoldingAiDetail;
import com.example.elephantfinancelab_be.domain.portfolio.entity.TradeType;
import com.example.elephantfinancelab_be.domain.portfolio.exception.PortfolioException;
import com.example.elephantfinancelab_be.domain.portfolio.exception.code.PortfolioErrorCode;
import com.example.elephantfinancelab_be.domain.portfolio.repository.HoldingAiDetailRepository;
import com.example.elephantfinancelab_be.domain.portfolio.repository.PositionRepository;
import com.example.elephantfinancelab_be.domain.portfolio.service.KisPortfolioClient;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryServiceImpl implements PortfolioQueryService {

  private final PositionRepository positionRepository;
  private final HoldingAiDetailRepository holdingAiDetailRepository;
  private final KisPortfolioClient kisPortfolioClient;

  @Override
  public PortfolioResDTO.Summary findSummary(Long userId) {
    return PortfolioConverter.toSummary(kisPortfolioClient.fetchBalance());
  }

  @Override
  public PortfolioResDTO.PositionPage findPositions(Long userId, Pageable pageable) {
    List<PortfolioResDTO.PositionDetail> details =
        kisPortfolioClient.fetchBalance().holdings().stream()
            .sorted((a, b) -> Long.compare(b.evaluationAmount(), a.evaluationAmount()))
            .map(PortfolioConverter::toPositionDetail)
            .toList();
    if (details.isEmpty()) {
      return PortfolioResDTO.PositionPage.builder()
          .page(pageable.getPageNumber())
          .size(pageable.getPageSize())
          .totalElements(0L)
          .totalPages(0)
          .hasNext(false)
          .positions(Collections.emptyList())
          .build();
    }
    return PortfolioConverter.toPositionPage(
        slice(details, pageable), pageable, details.size());
  }

  @Override
  public PortfolioResDTO.TradePage findTrades(
      Long userId, TradeType type, String period, Pageable pageable) {
    LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
    LocalDate startDate = startDateForPeriod(period, endDate);
    List<PortfolioResDTO.TradeDetail> trades =
        kisPortfolioClient.fetchTrades(startDate, endDate, type).stream()
            .map(PortfolioConverter::toTradeDetail)
            .toList();
    List<PortfolioResDTO.TradeDetail> pageContent = slice(trades, pageable);
    return PortfolioConverter.toTradePage(
        pageContent, pageable, pageable.getOffset() + pageContent.size() < trades.size());
  }

  @Override
  public PortfolioResDTO.HoldingAiDetail findHoldingDetail(Long userId, String tickerCode) {
    String normalizedTicker = tickerCode.toUpperCase();
    positionRepository
        .findByUserIdAndTickerCode(userId, normalizedTicker)
        .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.HOLDING404_01));
    HoldingAiDetail aiDetail =
        holdingAiDetailRepository
            .findTopByTickerCodeOrderByGeneratedAtDesc(normalizedTicker)
            .orElseThrow(() -> new PortfolioException(PortfolioErrorCode.AI_DETAIL404_01));
    return PortfolioConverter.toHoldingAiDetail(aiDetail);
  }

  private LocalDate startDateForPeriod(String period, LocalDate endDate) {
    if (period == null || period.isBlank() || "1M".equalsIgnoreCase(period)) {
      return endDate.minusMonths(1);
    }
    return switch (period.toUpperCase()) {
      case "1W" -> endDate.minusWeeks(1);
      case "3M" -> endDate.minusMonths(3);
      case "6M" -> endDate.minusMonths(6);
      case "1Y" -> endDate.minusYears(1);
      default -> endDate.minusMonths(1);
    };
  }

  private <T> List<T> slice(List<T> items, Pageable pageable) {
    int start = Math.toIntExact(Math.min(pageable.getOffset(), items.size()));
    int end = Math.min(start + pageable.getPageSize(), items.size());
    return items.subList(start, end);
  }
}
