package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

  private final UserRepository userRepository;

  @Override
  public void updateProfile(Long userId, UserReqDTO.UpdateProfile request) {
    User user = findActiveUser(userId);
    String normalizedPhone = normalizePhone(request.getPhone());
    if (!normalizedPhone.equals(user.getPhone())
        && userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
      throw new UserException(UserErrorCode.DUPLICATE_PHONE);
    }
    user.updateProfile(request.getName(), normalizedPhone);
  }

  @Override
  public void withdraw(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_ALREADY_DELETED);
    }
    user.withdraw();
  }

  private User findActiveUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND);
    }
    return user;
  }

  private static String normalizePhone(String phone) {
    return phone.replaceAll("\\D", "");
  }
}
