package com.example.elephantfinancelab_be.domain.terms.service.command;

import com.example.elephantfinancelab_be.domain.terms.dto.req.UserTermsReqDTO;
import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import com.example.elephantfinancelab_be.domain.terms.repository.UserTermsAgreementRepository;
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
public class UserTermsCommandServiceImpl implements UserTermsCommandService {

  private final UserRepository userRepository;
  private final UserTermsAgreementRepository userTermsAgreementRepository;

  @Override
  public void agreeAll(Long userId, UserTermsReqDTO.AgreeAll request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

    for (TermsType type : TermsType.values()) {
      UserTermsAgreement agreement =
          userTermsAgreementRepository
              .findByUser_IdAndTermsType(userId, type)
              .orElseGet(
                  () ->
                      UserTermsAgreement.builder()
                          .user(user)
                          .termsType(type)
                          .agreed(false)
                          .agreedAt(null)
                          .build());
      agreement.agree();
      userTermsAgreementRepository.save(agreement);
    }
  }
}
