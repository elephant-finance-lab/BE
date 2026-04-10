package com.example.elephantfinancelab_be.domain.terms.service.command;

import com.example.elephantfinancelab_be.domain.terms.dto.req.UserTermsReqDTO;

public interface UserTermsCommandService {

  void agreeAll(Long userId, UserTermsReqDTO.AgreeAll request);
}
