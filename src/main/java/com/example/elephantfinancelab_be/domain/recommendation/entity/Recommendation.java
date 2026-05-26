package com.example.elephantfinancelab_be.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "tickerCode"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Recommendation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String tickerCode;

  private String companyName;
  private String logoUrl;
  private Long currentPrice;
  private Double changeRate;
  private String currency;
  private Integer ranking;
  private Double score;

  @Column(name = "model_recommendation_id", length = 200)
  private String modelRecommendationId;

  @Column(name = "expected_return")
  private Double expectedReturn;

  @Column(name = "expected_return_available")
  private Boolean expectedReturnAvailable;

  @Column(name = "risk_level", length = 20)
  private String riskLevel;

  @Column(name = "model_version", length = 100)
  private String modelVersion;

  @Column(name = "model_bundle_id", length = 120)
  private String modelBundleId;

  @Column(name = "model_generated_at", length = 50)
  private String modelGeneratedAt;

  @Column(name = "model_asof", length = 50)
  private String modelAsof;

  @Column(columnDefinition = "TEXT")
  private String recommendReason;

  @Column(columnDefinition = "TEXT")
  private String companySummary;

  @Column(columnDefinition = "TEXT")
  private String growthPoint;

  @Column(columnDefinition = "TEXT")
  private String priceAttractiveness;

  @Column(columnDefinition = "TEXT")
  private String risk;

  public void updateModelResult(
      String modelRecommendationId,
      String stockName,
      Integer ranking,
      Double score,
      String recommendReason,
      Double expectedReturn,
      Boolean expectedReturnAvailable,
      String riskLevel,
      String modelVersion,
      String modelBundleId,
      String modelGeneratedAt,
      String modelAsof) {
    this.modelRecommendationId = modelRecommendationId;
    if (stockName != null && !stockName.isBlank()) {
      this.companyName = stockName;
    }
    this.ranking = ranking;
    this.score = score;
    this.recommendReason = recommendReason;
    this.expectedReturn = expectedReturn;
    this.expectedReturnAvailable = expectedReturnAvailable;
    this.riskLevel = riskLevel;
    this.modelVersion = modelVersion;
    this.modelBundleId = modelBundleId;
    this.modelGeneratedAt = modelGeneratedAt;
    this.modelAsof = modelAsof;
  }
}
