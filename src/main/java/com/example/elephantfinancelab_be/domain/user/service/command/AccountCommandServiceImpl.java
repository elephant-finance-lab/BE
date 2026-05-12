package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.converter.AccountConverter;
import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.Account;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.AccountException;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.AccountErrorCode;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.AccountRepository;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.domain.user.service.KisBalanceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountCommandServiceImpl implements AccountCommandService {

  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final KisBalanceClient kisBalanceClient;

  @Override
  public UserResDTO.AccountId saveAccount(Long userId, UserReqDTO.CreateAccount request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

    if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
      throw new AccountException(AccountErrorCode.ACCOUNT_ALREADY_EXISTS);
    }

    if (!kisBalanceClient.isValidAccount(request.getAccountNumber())) {
      throw new AccountException(AccountErrorCode.ACCOUNT_INVALID);
    }

    Account account = AccountConverter.toEntity(user, request);
    accountRepository.save(account);
    return AccountConverter.toAccountId(account);
  }

  @Override
  public void deleteAccount(Long userId, Long accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));

    accountRepository.delete(account);
  }
}
