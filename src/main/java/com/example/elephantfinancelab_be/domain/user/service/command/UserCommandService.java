package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;

public interface UserCommandService {

  void updateProfile(Long userId, UserReqDTO.UpdateProfile request);

  void withdraw(Long userId);

  UserResDTO.UserId saveUserInfo(Long userId, UserReqDTO.RegisterInfo request);
}
