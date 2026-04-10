package com.example.elephantfinancelab_be.domain.terms.repository;

import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAgreementRepository extends JpaRepository<UserTermsAgreement, Long> {

  List<UserTermsAgreement> findByUser_Id(Long userId);

  Optional<UserTermsAgreement> findByUser_IdAndTermsType(Long userId, TermsType termsType);
}
