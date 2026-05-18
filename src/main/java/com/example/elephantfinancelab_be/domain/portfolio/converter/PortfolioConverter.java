package com.example.elephantfinancelab_be.domain.portfolio.converter;

import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.Position;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PortfolioConverter {

  public static PortfolioResDTO.PositionDetail toPositionDetail(
      Position position, StockResDTO.Summary stockSummary) {
    long currentPrice =
        stockSummary != null ? stockSummary.getCurrentPriceKrw() : position.getAvgBuyPrice();
    long evalAmount = currentPrice * position.getQuantity();
    long profitAmount = evalAmount - position.getTotalBuyAmount();
    double profitRate =
        position.getTotalBuyAmount() == 0
            ? 0
            : Math.round((double) profitAmount / position.getTotalBuyAmount() * 10000.0) / 100.0;

    return PortfolioResDTO.PositionDetail.builder()
        .positionId(position.getId())
        .tickerCode(position.getTickerCode())
        .companyName(position.getCompanyName())
        .quantity(position.getQuantity())
        .avgBuyPrice(position.getAvgBuyPrice())
        .currentPrice(currentPrice)
        .totalBuyAmount(position.getTotalBuyAmount())
        .evalAmount(evalAmount)
        .profitAmount(profitAmount)
        .profitRate(profitRate)
        .openedAt(position.getOpenedAt())
        .build();
  }

  public static PortfolioResDTO.Summary toSummary(List<PortfolioResDTO.PositionDetail> details) {
    long totalAsset =
        details.stream().mapToLong(PortfolioResDTO.PositionDetail::getEvalAmount).sum();
    long totalBuy =
        details.stream().mapToLong(PortfolioResDTO.PositionDetail::getTotalBuyAmount).sum();
    long totalProfit = totalAsset - totalBuy;
    double totalProfitRate =
        totalBuy == 0 ? 0 : Math.round((double) totalProfit / totalBuy * 10000.0) / 100.0;

    List<PortfolioResDTO.PositionSummary> positionSummaries =
        details.stream()
            .map(
                d ->
                    PortfolioResDTO.PositionSummary.builder()
                        .tickerCode(d.getTickerCode())
                        .companyName(d.getCompanyName())
                        .quantity(d.getQuantity())
                        .evalAmount(d.getEvalAmount())
                        .profitRate(d.getProfitRate())
                        .weight(
                            totalAsset == 0
                                ? 0
                                : Math.round((double) d.getEvalAmount() / totalAsset * 100.0)
                                    / 100.0)
                        .build())
            .toList();

    return PortfolioResDTO.Summary.builder()
        .totalAsset(totalAsset)
        .totalProfit(totalProfit)
        .totalProfitRate(totalProfitRate)
        .positions(positionSummaries)
        .build();
  }

  public static PortfolioResDTO.PositionPage toPositionPage(
      Page<PortfolioResDTO.PositionDetail> page) {
    return PortfolioResDTO.PositionPage.builder()
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .hasNext(page.hasNext())
        .positions(page.getContent())
        .build();
  }
}
