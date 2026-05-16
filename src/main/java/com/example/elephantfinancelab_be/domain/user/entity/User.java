package com.example.elephantfinancelab_be.domain.user.entity;

import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "uuid", nullable = false, unique = true)
  private UUID uuid;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private Provider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "email", length = 255, unique = true, nullable = false)
  private String email;

  @Column(name = "phone", length = 20, unique = true)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 10)
  private Gender gender;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "avatar_url", columnDefinition = "TEXT")
  private String avatarUrl;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void updateProfile(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  public void withdraw() {
    this.deleted = true;
    this.active = false;
    this.deletedAt = LocalDateTime.now();
  }

  public void updateInfo(String name, String phone, Gender gender) {
    this.name = name;
    this.phone = phone;
    this.gender = gender;
  }
}
