package com.example.elephantfinancelab_be.domain.user.dto.req;

import com.example.elephantfinancelab_be.domain.user.entity.AccountType;
import com.example.elephantfinancelab_be.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserReqDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UpdateProfile {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    private String phone;
  }

  @Getter
  @NoArgsConstructor
  @Builder
  public static class Withdraw {}

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CreateAccount {

    @NotBlank(message = "예금주명은 필수입니다.")
    @Size(max = 100, message = "예금주명은 100자 이하여야 합니다.")
    private String accountHolder;

    @NotBlank(message = "은행명은 필수입니다.")
    @Size(max = 100, message = "은행명은 100자 이하여야 합니다.")
    private String bankName;

    @NotBlank(message = "계좌번호는 필수입니다.")
    @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
    private String accountNumber;

    @NotNull(message = "계좌 유형은 필수입니다.")
    private AccountType accountType;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RegisterInfo {

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    private String phone;

    @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
    private String accountNumber;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;
  }
}
