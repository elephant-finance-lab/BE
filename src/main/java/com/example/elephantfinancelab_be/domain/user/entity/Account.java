package com.example.elephantfinancelab_be.domain.user.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Account extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "bank_name", nullable = false, length = 100)
  private String bankName;

  @Column(name = "account_number", nullable = false, length = 50, unique = true)
  private String accountNumber;

  @Column(name = "account_holder", nullable = false, length = 100)
  private String accountHolder;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false, length = 20)
  private AccountType accountType;

  @Column(name = "is_primary", nullable = false)
  @Builder.Default
  private boolean primary = false;

  @Column(name = "is_hidden", nullable = false)
  @Builder.Default
  private boolean hidden = false;

  @Column(name = "balance", nullable = false)
  @Builder.Default
  private Long balance = 0L;
}
