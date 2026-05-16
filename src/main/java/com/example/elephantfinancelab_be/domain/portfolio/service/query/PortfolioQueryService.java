package com.example.elephantfinancelab_be.domain.portfolio.service.query;

import com.example.elephantfinancelab_be.domain.portfolio.dto.res.PortfolioResDTO;
import org.springframework.data.domain.Pageable;

public interface PortfolioQueryService {

  PortfolioResDTO.Summary findSummary(Long userId);

  PortfolioResDTO.PositionPage findPositions(Long userId, Pageable pageable);
}
