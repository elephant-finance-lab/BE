package com.example.elephantfinancelab_be.domain.user.service.query;

import com.example.elephantfinancelab_be.domain.user.converter.AccountConverter;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.repository.AccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountQueryServiceImpl implements AccountQueryService {

  private final AccountRepository accountRepository;

  @Override
  public List<UserResDTO.AccountInfo> findAccountList(Long userId) {
    return AccountConverter.toAccountInfoList(accountRepository.findAllByUserId(userId));
  }
}
