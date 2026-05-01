package com.example.elephantfinancelab_be.domain.recommendation.repository;

import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
  Optional<Recommendation> findByTickerCodeIgnoreCase(String tickerCode);
}
