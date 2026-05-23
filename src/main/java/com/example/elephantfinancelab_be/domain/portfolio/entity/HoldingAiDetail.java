package com.example.elephantfinancelab_be.domain.portfolio.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "holding_ai_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HoldingAiDetail extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ticker_code", nullable = false, length = 20)
  private String tickerCode;

  @Column(name = "company_name", nullable = false, length = 100)
  private String companyName;

  @Column(name = "ai_hit_rate", nullable = false)
  private Double aiHitRate;

  @Column(name = "trade_reason", nullable = false, columnDefinition = "TEXT")
  private String tradeReason;

  @Column(name = "future_strategy", nullable = false, columnDefinition = "TEXT")
  private String futureStrategy;

  @Column(name = "generated_at", nullable = false)
  private LocalDateTime generatedAt;
}
