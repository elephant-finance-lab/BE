package com.example.elephantfinancelab_be.domain.portfolio.converter;

import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import com.example.elephantfinancelab_be.domain.portfolio.entity.HoldingAiDetail;
import com.example.elephantfinancelab_be.domain.portfolio.entity.Position;
import com.example.elephantfinancelab_be.domain.portfolio.entity.Trade;
import com.example.elephantfinancelab_be.domain.portfolio.service.KisPortfolioClient;
import com.example.elephantfinancelab_be.domain.stocks.dto.res.StockResDTO;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

  public static PortfolioResDTO.PositionDetail toPositionDetail(
      KisPortfolioClient.Holding holding) {
    return PortfolioResDTO.PositionDetail.builder()
        .positionId(null)
        .tickerCode(holding.stockCode())
        .companyName(holding.stockName())
        .quantity(holding.quantity())
        .avgBuyPrice(holding.averagePrice())
        .currentPrice(holding.currentPrice())
        .totalBuyAmount(holding.purchaseAmount())
        .evalAmount(holding.evaluationAmount())
        .profitAmount(holding.profitLossAmount())
        .profitRate(holding.profitLossRate())
        .openedAt(null)
        .build();
  }

  public static PortfolioResDTO.Summary toSummary(KisPortfolioClient.Balance balance) {
    List<KisPortfolioClient.Holding> holdings =
        balance.holdings().stream()
            .sorted((a, b) -> Long.compare(b.evaluationAmount(), a.evaluationAmount()))
            .toList();
    long stockEvaluationAmount =
        balance.totals().stockEvaluationAmount() > 0
            ? balance.totals().stockEvaluationAmount()
            : holdings.stream().mapToLong(KisPortfolioClient.Holding::evaluationAmount).sum();
    List<PortfolioResDTO.PositionSummary> positionSummaries =
        holdings.stream()
            .map(holding -> toPositionSummary(holding, stockEvaluationAmount))
            .toList();

    return PortfolioResDTO.Summary.builder()
        .totalAsset(balance.totals().totalAssetAmount())
        .totalProfit(balance.totals().totalProfitLossAmount())
        .totalProfitRate(balance.totals().totalProfitLossRate())
        .positions(positionSummaries)
        .totalAssetAmount(balance.totals().totalAssetAmount())
        .stockEvaluationAmount(stockEvaluationAmount)
        .cashAmount(balance.totals().cashAmount())
        .totalProfitLossAmount(balance.totals().totalProfitLossAmount())
        .totalProfitLossRate(balance.totals().totalProfitLossRate())
        .holdings(positionSummaries)
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
                                : Math.round((double) d.getEvalAmount() / totalAsset * 10000.0)
                                    / 100.0)
                        .stockCode(d.getTickerCode())
                        .stockName(d.getCompanyName())
                        .averagePrice(d.getAvgBuyPrice())
                        .currentPrice(d.getCurrentPrice())
                        .evaluationAmount(d.getEvalAmount())
                        .profitLossAmount(d.getProfitAmount())
                        .profitLossRate(d.getProfitRate())
                        .weightRate(
                            totalAsset == 0
                                ? 0
                                : Math.round((double) d.getEvalAmount() / totalAsset * 10000.0)
                                    / 100.0)
                        .build())
            .toList();

    return PortfolioResDTO.Summary.builder()
        .totalAsset(totalAsset)
        .totalProfit(totalProfit)
        .totalProfitRate(totalProfitRate)
        .positions(positionSummaries)
        .totalAssetAmount(totalAsset)
        .stockEvaluationAmount(totalAsset)
        .cashAmount(0L)
        .totalProfitLossAmount(totalProfit)
        .totalProfitLossRate(totalProfitRate)
        .holdings(positionSummaries)
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

  public static PortfolioResDTO.PositionPage toPositionPage(
      List<PortfolioResDTO.PositionDetail> details, Pageable pageable, long totalElements) {
    int totalPages =
        pageable.getPageSize() == 0
            ? 0
            : (int) Math.ceil((double) totalElements / pageable.getPageSize());
    boolean hasNext =
        (long) (pageable.getPageNumber() + 1) * pageable.getPageSize() < totalElements;
    return PortfolioResDTO.PositionPage.builder()
        .page(pageable.getPageNumber())
        .size(pageable.getPageSize())
        .totalElements(totalElements)
        .totalPages(totalPages)
        .hasNext(hasNext)
        .positions(details)
        .build();
  }

  public static PortfolioResDTO.TradeDetail toTradeDetail(Trade trade) {
    return PortfolioResDTO.TradeDetail.builder()
        .tradeId(trade.getId())
        .tickerCode(trade.getTickerCode())
        .companyName(trade.getCompanyName())
        .type(trade.getType())
        .quantity(trade.getQuantity())
        .price(trade.getPrice())
        .totalAmount(trade.getTotalAmount())
        .tradedAt(trade.getTradedAt())
        .tradeDate(trade.getTradedAt().toLocalDate().toString())
        .side(trade.getType())
        .stockCode(trade.getTickerCode())
        .stockName(trade.getCompanyName())
        .amount(trade.getTotalAmount())
        .build();
  }

  public static PortfolioResDTO.TradeDetail toTradeDetail(KisPortfolioClient.TradeItem trade) {
    return PortfolioResDTO.TradeDetail.builder()
        .tradeId(null)
        .tickerCode(trade.stockCode())
        .companyName(trade.stockName())
        .type(trade.type())
        .quantity(trade.quantity())
        .price(trade.price())
        .totalAmount(trade.totalAmount())
        .tradedAt(trade.tradedAt())
        .tradeDate(trade.tradedAt().toLocalDate().toString())
        .side(trade.type())
        .stockCode(trade.stockCode())
        .stockName(trade.stockName())
        .amount(trade.totalAmount())
        .build();
  }

  public static PortfolioResDTO.TradePage toTradePage(Page<Trade> page) {
    List<PortfolioResDTO.TradeDetail> trades =
        page.getContent().stream().map(PortfolioConverter::toTradeDetail).toList();
    return PortfolioResDTO.TradePage.builder()
        .page(page.getNumber())
        .size(page.getSize())
        .hasNext(page.hasNext())
        .trades(trades)
        .items(trades)
        .build();
  }

  public static PortfolioResDTO.TradePage toTradePage(
      List<PortfolioResDTO.TradeDetail> trades, Pageable pageable, boolean hasNext) {
    return PortfolioResDTO.TradePage.builder()
        .page(pageable.getPageNumber())
        .size(pageable.getPageSize())
        .hasNext(hasNext)
        .trades(trades)
        .items(trades)
        .build();
  }

  public static PortfolioResDTO.HoldingAiDetail toHoldingAiDetail(HoldingAiDetail entity) {
    return PortfolioResDTO.HoldingAiDetail.builder()
        .tickerCode(entity.getTickerCode())
        .companyName(entity.getCompanyName())
        .aiHitRate(entity.getAiHitRate())
        .tradeReason(entity.getTradeReason())
        .futureStrategy(entity.getFutureStrategy())
        .generatedAt(entity.getGeneratedAt())
        .build();
  }

  private static PortfolioResDTO.PositionSummary toPositionSummary(
      KisPortfolioClient.Holding holding, long stockEvaluationAmount) {
    double weight =
        stockEvaluationAmount == 0
            ? 0.0
            : Math.round((double) holding.evaluationAmount() / stockEvaluationAmount * 10000.0)
                / 100.0;
    return PortfolioResDTO.PositionSummary.builder()
        .tickerCode(holding.stockCode())
        .companyName(holding.stockName())
        .quantity(holding.quantity())
        .evalAmount(holding.evaluationAmount())
        .profitRate(holding.profitLossRate())
        .weight(weight)
        .stockCode(holding.stockCode())
        .stockName(holding.stockName())
        .averagePrice(holding.averagePrice())
        .currentPrice(holding.currentPrice())
        .evaluationAmount(holding.evaluationAmount())
        .profitLossAmount(holding.profitLossAmount())
        .profitLossRate(holding.profitLossRate())
        .weightRate(weight)
        .build();
  }
}
