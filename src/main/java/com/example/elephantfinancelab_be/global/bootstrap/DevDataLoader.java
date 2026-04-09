package com.example.elephantfinancelab_be.global.bootstrap;

import com.example.elephantfinancelab_be.domain.user.entity.Gender;
import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataLoader implements ApplicationRunner {

  private static final Long DEV_USER_ID = 1L;
  private static final UUID DEV_USER_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

  private final UserRepository userRepository;

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsById(DEV_USER_ID)) {
      return;
    }

    // 시드 데이터 추가
    if (userRepository.count() > 0) {
      return;
    }

    User user =
        User.builder()
            .uuid(DEV_USER_UUID)
            .provider(Provider.GOOGLE)
            .providerUserId("dev-google-provider-user-id")
            .name("김이박")
            .phone("01012345678")
            .gender(Gender.MALE)
            .birthDate(null)
            .avatarUrl(null)
            .active(true)
            .deleted(false)
            .deletedAt(null)
            .build();
    userRepository.save(user);
  }
}
