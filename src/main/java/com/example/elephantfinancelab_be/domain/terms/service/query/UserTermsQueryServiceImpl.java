package com.example.elephantfinancelab_be.domain.terms.service.query;

import com.example.elephantfinancelab_be.domain.terms.converter.UserTermsConverter;
import com.example.elephantfinancelab_be.domain.terms.dto.res.UserTermsResDTO;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import com.example.elephantfinancelab_be.domain.terms.repository.UserTermsAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTermsQueryServiceImpl implements UserTermsQueryService {

  private final UserTermsAgreementRepository userTermsAgreementRepository;

  @Override
  public UserTermsResDTO.MyTerms getMyTerms(Long userId) {
    List<UserTermsAgreement> rows = userTermsAgreementRepository.findByUser_Id(userId);
    return UserTermsConverter.toMyTermsRes(rows);
  }
}
