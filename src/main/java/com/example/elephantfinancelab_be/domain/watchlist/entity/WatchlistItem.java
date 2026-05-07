package com.example.elephantfinancelab_be.domain.watchlist.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_watchlist_items_group_id_ticker",
            columnNames = {"group_id", "ticker"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WatchlistItem extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private WatchlistGroup group;

  @Column(name = "ticker", nullable = false, length = 20)
  private String ticker;
}
