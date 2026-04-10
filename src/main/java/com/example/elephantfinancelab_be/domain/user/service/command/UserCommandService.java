package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;

public interface UserCommandService {

  void updateProfile(Long userId, UserReqDTO.UpdateProfile request);

  void withdraw(Long userId);
}
