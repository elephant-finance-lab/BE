package com.example.elephantfinancelab_be.domain.user.service.query;

import com.example.elephantfinancelab_be.domain.user.converter.UserConverter;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

  private final UserRepository userRepository;

  @Override
  public UserResDTO.Profile getMyProfile(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND);
    }
    return UserConverter.toProfileRes(user);
  }
}
