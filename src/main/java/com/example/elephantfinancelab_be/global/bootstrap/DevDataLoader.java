package com.example.elephantfinancelab_be.global.bootstrap;

import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import com.example.elephantfinancelab_be.domain.terms.repository.UserTermsAgreementRepository;
import com.example.elephantfinancelab_be.domain.user.entity.Gender;
import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DevDataLoader implements ApplicationRunner {

  // TODO: 하드코딩 해제
  private static final Long DEV_USER_ID = 1L;
  private static final UUID DEV_USER_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

  private final UserRepository userRepository;
  private final UserTermsAgreementRepository userTermsAgreementRepository;

  @Override
  public void run(ApplicationArguments args) {
    User user = userRepository.findById(DEV_USER_ID).orElse(null);
    if (user == null) {
      if (userRepository.count() > 0) {
        return;
      }
      user = userRepository.save(createDevUser());
    }
    if (!DEV_USER_UUID.equals(user.getUuid())) {
      return;
    }
    seedTermsAgreementsIfMissing(user);
  }

  private static User createDevUser() {
    return User.builder()
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
  }

  private void seedTermsAgreementsIfMissing(User user) {
    LocalDateTime now = LocalDateTime.now();
    for (TermsType type : TermsType.values()) {
      if (userTermsAgreementRepository.findByUser_IdAndTermsType(user.getId(), type).isPresent()) {
        continue;
      }
      userTermsAgreementRepository.save(
          UserTermsAgreement.builder()
              .user(user)
              .termsType(type)
              .agreed(true)
              .agreedAt(now)
              .build());
    }
  }
}
