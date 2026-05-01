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
}
