package com.example.elephantfinancelab_be.domain.user.converter;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.Account;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountConverter {

  public static Account toEntity(User user, UserReqDTO.CreateAccount request) {
    return Account.builder()
        .user(user)
        .bankName(request.getBankName())
        .accountNumber(request.getAccountNumber())
        .accountHolder(request.getAccountHolder())
        .accountType(request.getAccountType())
        .build();
  }

  public static UserResDTO.AccountInfo toAccountInfo(Account account) {
    return UserResDTO.AccountInfo.builder()
        .accountId(account.getId())
        .bankName(account.getBankName())
        .accountNumber(account.getAccountNumber())
        .accountHolder(account.getAccountHolder())
        .accountType(account.getAccountType())
        .isPrimary(account.isPrimary())
        .isHidden(account.isHidden())
        .balance(account.getBalance())
        .linkedAt(account.getCreatedAt())
        .build();
  }

  public static List<UserResDTO.AccountInfo> toAccountInfoList(List<Account> accounts) {
    return accounts.stream().map(AccountConverter::toAccountInfo).toList();
  }

  public static UserResDTO.AccountId toAccountId(Account account) {
    return UserResDTO.AccountId.builder().accountId(account.getId()).build();
  }
}
