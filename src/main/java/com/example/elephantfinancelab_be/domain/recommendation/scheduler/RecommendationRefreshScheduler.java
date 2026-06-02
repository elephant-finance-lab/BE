package com.example.elephantfinancelab_be.domain.recommendation.scheduler;

import com.example.elephantfinancelab_be.domain.recommendation.dto.res.RecommendationResDTO;
import com.example.elephantfinancelab_be.domain.recommendation.service.query.RecommendationQueryService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecommendationRefreshScheduler {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final RecommendationQueryService recommendationQueryService;
  private final Clock clock;

  @Value("${ai.recommendations.refresh.enabled:false}")
  private boolean refreshEnabled;

  @Value("${ai.recommendations.refresh.market-hours-only:true}")
  private boolean marketHoursOnly;

  @Value("${ai.recommendations.refresh.market-open:09:00}")
  private String marketOpen;

  @Value("${ai.recommendations.refresh.market-close:15:30}")
  private String marketClose;

  @Value("${ai.recommendations.refresh.market-holidays:}")
  private String marketHolidays;

  private volatile ParsedMarketSchedule parsedMarketSchedule;

  @Autowired
  public RecommendationRefreshScheduler(RecommendationQueryService recommendationQueryService) {
    this(recommendationQueryService, Clock.system(KOREA_ZONE));
  }

  RecommendationRefreshScheduler(
      RecommendationQueryService recommendationQueryService, Clock clock) {
    this.recommendationQueryService = recommendationQueryService;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${ai.recommendations.refresh.fixed-delay-ms:60000}",
      initialDelayString = "${ai.recommendations.refresh.initial-delay-ms:10000}")
  public void refreshRecommendations() {
    if (!refreshEnabled) {
      return;
    }

    LocalDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(KOREA_ZONE).toLocalDateTime();
    if (!shouldRefreshAt(now)) {
      return;
    }

    try {
      RecommendationResDTO.RecommendationListDTO refreshed =
          recommendationQueryService.refreshModelRecommendations();
      int count =
          refreshed.getRecommendations() == null ? 0 : refreshed.getRecommendations().size();
      log.info(
          "[RecommendationRefresh] AI recommendations refreshed: status={}, count={}, asof={}",
          refreshed.getModelStatus(),
          count,
          refreshed.getAsof());
    } catch (RuntimeException e) {
      log.warn("[RecommendationRefresh] AI recommendation refresh skipped: {}", e.getMessage(), e);
    }
  }

  boolean shouldRefreshAt(LocalDateTime now) {
    if (!marketHoursOnly) {
      return true;
    }
    if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY) {
      return false;
    }
    if (isMarketHoliday(now.toLocalDate())) {
      return false;
    }

    ParsedMarketSchedule schedule = parsedMarketSchedule();
    LocalTime open = schedule.open();
    LocalTime close = schedule.close();
    if (open == null || close == null || close.isBefore(open)) {
      return false;
    }

    LocalTime time = now.toLocalTime();
    return !time.isBefore(open) && !time.isAfter(close);
  }

  private ParsedMarketSchedule parsedMarketSchedule() {
    ParsedMarketSchedule cached = parsedMarketSchedule;
    if (cached != null && cached.matches(marketOpen, marketClose, marketHolidays)) {
      return cached;
    }

    ParsedMarketSchedule parsed =
        new ParsedMarketSchedule(
            marketOpen,
            marketClose,
            marketHolidays,
            parseMarketTime(marketOpen),
            parseMarketTime(marketClose),
            parseMarketHolidays(marketHolidays));
    parsedMarketSchedule = parsed;
    return parsed;
  }

  private LocalTime parseMarketTime(String raw) {
    if (raw == null) {
      return null;
    }
    try {
      return LocalTime.parse(raw);
    } catch (DateTimeParseException e) {
      log.warn("[RecommendationRefresh] Invalid market time config: {}", raw);
      return null;
    }
  }

  private boolean isMarketHoliday(LocalDate date) {
    return parsedMarketSchedule().holidays().contains(date);
  }

  private Set<LocalDate> parseMarketHolidays(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .map(this::parseMarketHoliday)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private LocalDate parseMarketHoliday(String raw) {
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      log.warn("[RecommendationRefresh] Invalid market holiday config: {}", raw);
      return null;
    }
  }

  private record ParsedMarketSchedule(
      String marketOpen,
      String marketClose,
      String marketHolidays,
      LocalTime open,
      LocalTime close,
      Set<LocalDate> holidays) {

    private boolean matches(String marketOpen, String marketClose, String marketHolidays) {
      return Objects.equals(this.marketOpen, marketOpen)
          && Objects.equals(this.marketClose, marketClose)
          && Objects.equals(this.marketHolidays, marketHolidays);
    }
  }
}
