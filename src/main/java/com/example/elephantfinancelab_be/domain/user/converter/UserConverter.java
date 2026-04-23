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

  public static UserResDTO.MeRes toMeRes(String userId, String accessToken) {
    return UserResDTO.MeRes.builder().userId(userId).accessToken(accessToken).build();
  }

  public static UserResDTO.MeRes toMeRes(String userId) {
    return UserResDTO.MeRes.builder().userId(userId).build();
  }

  public static UserResDTO.TokenRes toTokenRes(String accessToken) {
    return UserResDTO.TokenRes.builder().accessToken(accessToken).build();
  }
}
