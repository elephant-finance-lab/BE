package com.example.elephantfinancelab_be.domain.recommendation.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.service.query.RecommendationQueryService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationRefreshSchedulerTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final RecommendationQueryService recommendationQueryService =
      mock(RecommendationQueryService.class);
  private final RecommendationRefreshScheduler scheduler =
      new RecommendationRefreshScheduler(
          recommendationQueryService, Clock.fixed(Instant.parse("2026-06-01T01:00:00Z"), KST));

  @Test
  void doesNothingWhenRefreshDisabled() {
    ReflectionTestUtils.setField(scheduler, "refreshEnabled", false);

    scheduler.refreshRecommendations();

    verifyNoInteractions(recommendationQueryService);
  }

  @Test
  void refreshesDuringConfiguredMarketHours() {
    ReflectionTestUtils.setField(scheduler, "refreshEnabled", true);
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", true);
    ReflectionTestUtils.setField(scheduler, "marketOpen", "09:00");
    ReflectionTestUtils.setField(scheduler, "marketClose", "15:30");
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");

    scheduler.refreshRecommendations();

    verify(recommendationQueryService).refreshModelRecommendations();
  }

  @Test
  void skipsOutsideMarketHours() {
    ReflectionTestUtils.setField(scheduler, "refreshEnabled", true);
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", true);
    ReflectionTestUtils.setField(scheduler, "marketOpen", "10:30");
    ReflectionTestUtils.setField(scheduler, "marketClose", "15:30");
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");

    scheduler.refreshRecommendations();

    verifyNoInteractions(recommendationQueryService);
  }

  @Test
  void canDisableMarketHourFilterForManualRuns() {
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", false);

    assertThat(scheduler.shouldRefreshAt(LocalDateTime.of(2026, 6, 6, 1, 0))).isTrue();
  }

  @Test
  void skipsConfiguredMarketHoliday() {
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", true);
    ReflectionTestUtils.setField(scheduler, "marketOpen", "09:00");
    ReflectionTestUtils.setField(scheduler, "marketClose", "15:30");
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");

    assertThat(scheduler.shouldRefreshAt(LocalDateTime.of(2026, 6, 3, 10, 0))).isFalse();
  }

  @Test
  void swallowRefreshFailureSoSchedulerKeepsRunning() {
    ReflectionTestUtils.setField(scheduler, "refreshEnabled", true);
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", true);
    ReflectionTestUtils.setField(scheduler, "marketOpen", "09:00");
    ReflectionTestUtils.setField(scheduler, "marketClose", "15:30");
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");
    doThrow(new IllegalStateException("ai down"))
        .when(recommendationQueryService)
        .refreshModelRecommendations();

    scheduler.refreshRecommendations();

    verify(recommendationQueryService).refreshModelRecommendations();
  }

  @Test
  void logsRefreshedRecommendationCountWhenServiceReturnsPayload() {
    ReflectionTestUtils.setField(scheduler, "refreshEnabled", true);
    ReflectionTestUtils.setField(scheduler, "marketHoursOnly", true);
    ReflectionTestUtils.setField(scheduler, "marketOpen", "09:00");
    ReflectionTestUtils.setField(scheduler, "marketClose", "15:30");
    ReflectionTestUtils.setField(scheduler, "marketHolidays", "2026-06-03,2026-07-17");
    RecommendationResDTO.RecommendationInfoDTO item =
        RecommendationResDTO.RecommendationInfoDTO.builder().stockCode("005930").build();
    org.mockito.Mockito.when(recommendationQueryService.refreshModelRecommendations())
        .thenReturn(
            RecommendationResDTO.RecommendationListDTO.builder()
                .modelStatus("PASS")
                .asof("2026-06-01T10:00:00+09:00")
                .recommendations(List.of(item))
                .build());

    scheduler.refreshRecommendations();

    verify(recommendationQueryService).refreshModelRecommendations();
  }
}
