package com.example.elephantfinancelab_be.domain.user.service.command;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.AccountType;
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
  private final AccountCommandService accountCommandService;

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

  @Override
  public UserResDTO.UserId saveUserInfo(Long userId, UserReqDTO.RegisterInfo request) {
    User user = findActiveUser(userId);

    if (user.getPhone() != null) {
      throw new UserException(UserErrorCode.ALREADY_REGISTERED);
    }

    String normalizedPhone = normalizePhone(request.getPhone());
    if (userRepository.existsByPhoneAndIdNot(normalizedPhone, userId)) {
      throw new UserException(UserErrorCode.DUPLICATE_PHONE);
    }
    user.updateInfo(request.getName(), normalizedPhone, request.getGender());

    if (request.getAccountNumber() != null && !request.getAccountNumber().isBlank()) {
      // 현재 KIS 모의투자(한국투자증권) 계좌만 지원. 추후 다른 증권사 연동 시 bankName, accountType 확장 필요
      UserReqDTO.CreateAccount accountRequest =
          UserReqDTO.CreateAccount.builder()
              .accountNumber(request.getAccountNumber())
              .accountHolder(user.getName())
              .bankName("한국투자증권")
              .accountType(AccountType.SECURITIES)
              .build();
      accountCommandService.saveAccount(userId, accountRequest);
    }

    return UserResDTO.UserId.builder().userId(userId).build();
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
