package com.example.elephantfinancelab_be.domain.user.service.query;

import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;

public interface UserQueryService {

  UserResDTO.Profile getMyProfile(Long userId);
}
