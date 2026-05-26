package com.example.elephantfinancelab_be.domain.stocks.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "stock_daily_price_snapshots",
    uniqueConstraints = @UniqueConstraint(columnNames = "stock_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StockDailyPriceSnapshot extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_id", nullable = false)
  private Stock stock;

  @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
  private String responseJson;

  public void updateResponseJson(String responseJson) {
    this.responseJson = responseJson;
  }
}
