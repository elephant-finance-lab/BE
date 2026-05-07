package com.example.elephantfinancelab_be.domain.recommendation.repository;

import com.example.elephantfinancelab_be.domain.recommendation.entity.UserSelectedRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSelectedRecommendationRepository
    extends JpaRepository<UserSelectedRecommendation, Long> {}
