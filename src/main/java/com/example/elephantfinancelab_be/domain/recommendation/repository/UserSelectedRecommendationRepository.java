package com.example.elephantfinancelab_be.domain.recommendation.repository;

import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSelectedRecommendationRepository
    extends JpaRepository<UserSelectedRecommendation, Long> {
  @EntityGraph(attributePaths = "recommendation")
  List<UserSelectedRecommendation> findAllByUserIdAndRecommendation_IdIn(
      Long userId, Collection<Long> recommendationIds);
}
