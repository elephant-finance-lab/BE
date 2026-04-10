package com.example.elephantfinancelab_be.domain.terms.service.query;

import com.example.elephantfinancelab_be.domain.terms.dto.res.UserTermsResDTO;

public interface UserTermsQueryService {

  UserTermsResDTO.MyTerms getMyTerms(Long userId);
}
