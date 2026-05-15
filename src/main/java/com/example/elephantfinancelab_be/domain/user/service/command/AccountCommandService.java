package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;

public interface AccountCommandService {

  UserResDTO.AccountId saveAccount(Long userId, UserReqDTO.CreateAccount request);

  void deleteAccount(Long userId, Long accountId);
}
