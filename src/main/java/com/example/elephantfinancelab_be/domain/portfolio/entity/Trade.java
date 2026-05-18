package com.example.elephantfinancelab_be.domain.portfolio.entity;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Trade extends BaseEntity {

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

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 10)
  private TradeType type;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "total_amount", nullable = false)
  private Long totalAmount;

  @Column(name = "traded_at", nullable = false)
  private LocalDateTime tradedAt;
}
