package com.example.elephantfinancelab_be.global.bootstrap;

import com.example.elephantfinancelab_be.domain.recommendation.entity.Recommendation;
import com.example.elephantfinancelab_be.domain.recommendation.repository.RecommendationRepository;
import com.example.elephantfinancelab_be.domain.stocks.entity.Stock;
import com.example.elephantfinancelab_be.domain.stocks.repository.StockRepository;
import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import com.example.elephantfinancelab_be.domain.terms.repository.UserTermsAgreementRepository;
import com.example.elephantfinancelab_be.domain.user.entity.Gender;
import com.example.elephantfinancelab_be.domain.user.entity.Provider;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
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
  private final StockRepository stockRepository;
  private final RecommendationRepository recommendationRepository;

  @Override
  public void run(ApplicationArguments args) {
    seedStocksIfMissing();
    seedRecommendationsIfMissing();

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
        .email("dev@elephant.com")
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

  private void seedStocksIfMissing() {
    if (stockRepository.count() > 0) {
      return;
    }

    stockRepository.saveAll(
        List.of(
            Stock.builder().ticker("005930").name("삼성전자").build(),
            Stock.builder().ticker("000660").name("SK하이닉스").build(),
            Stock.builder().ticker("035420").name("NAVER").build(),
            Stock.builder().ticker("035720").name("카카오").build(),
            Stock.builder().ticker("005380").name("현대차").build(),
            Stock.builder().ticker("373220").name("LG에너지솔루션").build()));
  }

  private void seedRecommendationsIfMissing() {
    if (recommendationRepository.count() > 0) {
      return;
    }

    recommendationRepository.saveAll(
        List.of(
            Recommendation.builder()
                .tickerCode("005930")
                .companyName("삼성전자")
                .currentPrice(72500L)
                .changeRate(1.24)
                .currency("KRW")
                .ranking(1)
                .score(0.94)
                .recommendReason("반도체 업황 회복과 안정적인 사업 구성을 반영한 추천입니다.")
                .companySummary("메모리 반도체와 모바일 사업을 영위하는 국내 대표 기업입니다.")
                .growthPoint("AI 서버 수요 확대에 따른 메모리 반도체 수요 회복")
                .priceAttractiveness("장기 성장성 대비 부담이 낮은 가격 구간")
                .risk("반도체 업황과 환율 변동에 따라 실적이 달라질 수 있습니다.")
                .build(),
            Recommendation.builder()
                .tickerCode("000660")
                .companyName("SK하이닉스")
                .currentPrice(198000L)
                .changeRate(2.31)
                .currency("KRW")
                .ranking(2)
                .score(0.91)
                .recommendReason("고대역폭 메모리 수요 증가가 기대되는 종목입니다.")
                .companySummary("메모리 반도체 중심의 글로벌 반도체 기업입니다.")
                .growthPoint("AI 가속기용 고대역폭 메모리 공급 확대")
                .priceAttractiveness("성장 기대를 감안한 중장기 관심 구간")
                .risk("메모리 가격 변동과 설비 투자 부담이 있습니다.")
                .build(),
            Recommendation.builder()
                .tickerCode("035420")
                .companyName("NAVER")
                .currentPrice(214000L)
                .changeRate(0.86)
                .currency("KRW")
                .ranking(3)
                .score(0.87)
                .recommendReason("플랫폼 사업과 AI 서비스 확장 가능성을 반영했습니다.")
                .companySummary("검색, 커머스, 콘텐츠와 클라우드 서비스를 운영하는 플랫폼 기업입니다.")
                .growthPoint("생성형 AI 및 커머스 수익화 확대")
                .priceAttractiveness("플랫폼 이익 회복 시 재평가 가능성")
                .risk("광고 경기와 신규 AI 투자 비용의 영향을 받습니다.")
                .build(),
            Recommendation.builder()
                .tickerCode("005380")
                .companyName("현대차")
                .currentPrice(247000L)
                .changeRate(-0.43)
                .currency("KRW")
                .ranking(4)
                .score(0.84)
                .recommendReason("글로벌 판매 기반과 친환경차 경쟁력을 반영했습니다.")
                .companySummary("승용차와 전기차를 생산 및 판매하는 글로벌 완성차 기업입니다.")
                .growthPoint("전기차 및 하이브리드 제품군 확대")
                .priceAttractiveness("수익성과 주주환원을 고려한 관심 구간")
                .risk("글로벌 수요 둔화와 관세 정책 변동 위험이 있습니다.")
                .build()));
  }
}
