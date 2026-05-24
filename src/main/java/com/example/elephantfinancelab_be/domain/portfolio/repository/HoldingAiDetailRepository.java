package com.example.elephantfinancelab_be.domain.portfolio.repository;

import com.example.elephantfinancelab_be.domain.portfolio.entity.HoldingAiDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingAiDetailRepository extends JpaRepository<HoldingAiDetail, Long> {

  Optional<HoldingAiDetail> findTopByTickerCodeOrderByGeneratedAtDesc(String tickerCode);
}
