package com.example.elephantfinancelab_be.domain.recommendation.repository;

import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
  Optional<Recommendation> findByTickerCodeIgnoreCase(String tickerCode);

  List<Recommendation> findAllByOrderByRankingAsc();

  List<Recommendation> findByModelGeneratedAtIsNotNull();

  List<Recommendation> findByModelGeneratedAtAndModelBundleIdOrderByRankingAsc(
      OffsetDateTime modelGeneratedAt, String modelBundleId);
}
