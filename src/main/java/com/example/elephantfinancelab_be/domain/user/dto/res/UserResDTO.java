package com.example.elephantfinancelab_be.domain.user.dto.res;

import com.example.elephantfinancelab_be.domain.user.entity.AccountType;
import com.example.elephantfinancelab_be.domain.user.entity.Gender;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserResDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Profile {
    private UUID uuid;
    private String name;
    private String phone;
    private Gender gender;
    private String avatarUrl;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MeRes {
    private String userId;
    private String accessToken;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TokenRes {
    private String accessToken;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountInfo {
    private Long accountId;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private AccountType accountType;
    private boolean primary;
    private boolean hidden;
    private Long balance;
    private LocalDateTime linkedAt;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountId {
    private Long accountId;
  }
}
