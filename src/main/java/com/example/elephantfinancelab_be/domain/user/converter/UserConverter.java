package com.example.elephantfinancelab_be.domain.user.converter;

import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserConverter {

  public static UserResDTO.Profile toProfileRes(User user) {
    return UserResDTO.Profile.builder()
        .uuid(user.getUuid())
        .name(user.getName())
        .phone(user.getPhone())
        .gender(user.getGender())
        .avatarUrl(user.getAvatarUrl())
        .build();
  }
}
