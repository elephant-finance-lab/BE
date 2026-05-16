package com.example.elephantfinancelab_be.domain.portfolio.entity;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "positions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Position extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "ticker_code", nullable = false, length = 20)
  private String tickerCode;

  @Column(name = "company_name", nullable = false, length = 100)
  private String companyName;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "avg_buy_price", nullable = false)
  private Long avgBuyPrice;

  @Column(name = "total_buy_amount", nullable = false)
  private Long totalBuyAmount;

  @Column(name = "opened_at", nullable = false)
  private LocalDateTime openedAt;
}
