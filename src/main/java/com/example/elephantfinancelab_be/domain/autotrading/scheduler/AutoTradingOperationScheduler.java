package com.example.elephantfinancelab_be.domain.autotrading.scheduler;

import com.elephant.ai.v1.ServiceReadinessResponse;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEvent;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingEventType;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSession;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingEventRepository;
import com.example.elephantfinancelab_be.domain.autotrading.repository.AutoTradingSessionRepository;
import com.example.elephantfinancelab_be.domain.autotrading.service.command.AutoTradingCommandService;
import com.example.elephantfinancelab_be.domain.autotrading.service.query.AutoTradingQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.config.AiServerClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AutoTradingOperationScheduler {

  private static final String ACTIVE_SLOT = "SHARED_KIS_VIRTUAL_ACCOUNT";
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final AutoTradingCommandService autoTradingCommandService;
  private final AutoTradingQueryService autoTradingQueryService;
  private final AutoTradingSessionRepository autoTradingSessionRepository;
  private final AutoTradingEventRepository autoTradingEventRepository;
  private final UserRepository userRepository;
  private final AiServerClient aiServerClient;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final AtomicBoolean operationLock = new AtomicBoolean(false);

  @Value("${auto-trading.operation.enabled:false}")
  private boolean operationEnabled;

  @Value("${auto-trading.operation.operator-email:}")
  private String operatorEmail;

  @Value("${auto-trading.operation.dry-run:true}")
  private boolean dryRun;

  @Value("${auto-trading.operation.purchase-option-id:2}")
  private int purchaseOptionId;

  @Value("${auto-trading.operation.cycles:390}")
  private int cycles;

  @Value("${auto-trading.operation.interval-sec:60}")
  private int intervalSec;

  @Value("${auto-trading.operation.retry-attempts:2}")
  private int retryAttempts;

  @Value("${auto-trading.operation.retry-backoff-ms:1000}")
  private long retryBackoffMs;

  @Value("${auto-trading.operation.market-holidays:2026-06-03,2026-07-17}")
  private String marketHolidays;

  @Value("${ai.paper-auto.bundle-id:}")
  private String bundleId;

  private volatile ParsedMarketHolidays parsedMarketHolidays;

  @Autowired
  public AutoTradingOperationScheduler(
      AutoTradingCommandService autoTradingCommandService,
      AutoTradingQueryService autoTradingQueryService,
      AutoTradingSessionRepository autoTradingSessionRepository,
      UserRepository userRepository,
      AiServerClient aiServerClient,
      AutoTradingEventRepository autoTradingEventRepository,
      ObjectMapper objectMapper) {
    this(
        autoTradingCommandService,
        autoTradingQueryService,
        autoTradingSessionRepository,
        userRepository,
        aiServerClient,
        autoTradingEventRepository,
        objectMapper,
        Clock.system(KOREA_ZONE));
  }

  AutoTradingOperationScheduler(
      AutoTradingCommandService autoTradingCommandService,
      AutoTradingQueryService autoTradingQueryService,
      AutoTradingSessionRepository autoTradingSessionRepository,
      UserRepository userRepository,
      AiServerClient aiServerClient,
      AutoTradingEventRepository autoTradingEventRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.autoTradingCommandService = autoTradingCommandService;
    this.autoTradingQueryService = autoTradingQueryService;
    this.autoTradingSessionRepository = autoTradingSessionRepository;
    this.userRepository = userRepository;
    this.aiServerClient = aiServerClient;
    this.autoTradingEventRepository = autoTradingEventRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Scheduled(
      cron = "${auto-trading.operation.pre-open-cron:0 30 8 * * MON-FRI}",
      zone = "Asia/Seoul")
  public void runPreOpenReadinessCheck() {
    if (!operationEnabled) {
      return;
    }
    if (!tryEnterOperation("pre_open_readiness")) {
      return;
    }
    LocalDateTime now = nowKst();
    try {
      if (!isTradingDay(now.toLocalDate())) {
        audit(
            "pre_open_readiness",
            "SKIPPED",
            "non_trading_day",
            Map.of("date", now.toLocalDate().toString()));
        log.info("[AutoTradingOperation] pre-open skipped: non-trading day {}", now.toLocalDate());
        return;
      }
      Optional<String> blocker = validateOperationConfig(false);
      if (blocker.isPresent()) {
        audit("pre_open_readiness", "BLOCKED", blocker.get(), Map.of());
        log.warn("[AutoTradingOperation] pre-open blocked by invalid config: {}", blocker.get());
        return;
      }
      ServiceReadinessResponse readiness =
          withRetry("pre_open_readiness", () -> aiServerClient.getServiceReadiness(bundleId));
      audit(
          "pre_open_readiness",
          readiness.getStatus(),
          "",
          Map.of(
              "bundleId",
              bundleId,
              "safeToEnableOrderActions",
              readiness.getSafeToEnableOrderActions(),
              "safeToEnableLiveActions",
              readiness.getSafeToEnableLiveActions(),
              "liveTradingAllowed",
              readiness.getLiveTradingAllowed(),
              "registryMutated",
              readiness.getRegistryMutated()));
      log.info(
          "[AutoTradingOperation] pre-open readiness: status={}, orderActions={}, liveActions={}, liveAllowed={}, registryMutated={}",
          readiness.getStatus(),
          readiness.getSafeToEnableOrderActions(),
          readiness.getSafeToEnableLiveActions(),
          readiness.getLiveTradingAllowed(),
          readiness.getRegistryMutated());
    } catch (RuntimeException e) {
      audit("pre_open_readiness", "ERROR", e.getMessage(), Map.of("bundleId", safeText(bundleId)));
      log.warn("[AutoTradingOperation] pre-open readiness check failed: {}", e.getMessage(), e);
    } finally {
      exitOperation();
    }
  }

  @Scheduled(cron = "${auto-trading.operation.start-cron:0 1 9 * * MON-FRI}", zone = "Asia/Seoul")
  public void startDailyPaperAuto() {
    if (!operationEnabled) {
      return;
    }
    if (!tryEnterOperation("daily_start")) {
      return;
    }
    LocalDateTime now = nowKst();
    try {
      if (!isTradingDay(now.toLocalDate())) {
        audit("daily_start", "SKIPPED", "non_trading_day", Map.of("date", now.toLocalDate()));
        log.info("[AutoTradingOperation] start skipped: non-trading day {}", now.toLocalDate());
        return;
      }
      Optional<String> blocker = validateOperationConfig(true);
      if (blocker.isPresent()) {
        audit("daily_start", "BLOCKED", blocker.get(), Map.of());
        log.warn("[AutoTradingOperation] start blocked by invalid config: {}", blocker.get());
        return;
      }
      if (findActiveSession().isPresent()) {
        audit("daily_start", "SKIPPED", "active_session_exists", Map.of());
        log.info("[AutoTradingOperation] start skipped: active paper-auto session already exists");
        return;
      }
      Optional<User> operator = findOperator();
      if (operator.isEmpty()) {
        audit("daily_start", "BLOCKED", "operator_user_not_found", Map.of());
        return;
      }

      String idempotencyKey = "server-paper-auto-" + now.toLocalDate();
      if (dryRun) {
        audit(
            "daily_start",
            "DRY_RUN",
            "dry_run_enabled",
            Map.of(
                "userId", operator.get().getId(),
                "idempotencyKey", idempotencyKey,
                "bundleId", bundleId,
                "cycles", cycles,
                "intervalSec", intervalSec));
        log.info(
            "[AutoTradingOperation] dry-run start skipped: userId={}, activeUniverse=true, bundleId={}",
            operator.get().getId(),
            bundleId);
        return;
      }
      AutoTradingResDTO.Session session =
          autoTradingCommandService.startActiveUniverseSession(
              operator.get().getId(), idempotencyKey, purchaseOptionId, cycles, intervalSec);
      audit(
          "daily_start",
          "REQUESTED",
          "",
          Map.of(
              "sessionId",
              session.getSessionId(),
              "status",
              String.valueOf(session.getStatus()),
              "bundleId",
              bundleId,
              "idempotencyKey",
              idempotencyKey));
      log.info(
          "[AutoTradingOperation] paper-auto start requested: sessionId={}, status={}, activeUniverse=true",
          session.getSessionId(),
          session.getStatus());
    } catch (RuntimeException e) {
      audit("daily_start", "ERROR", e.getMessage(), Map.of("bundleId", safeText(bundleId)));
      log.warn("[AutoTradingOperation] paper-auto start failed: {}", e.getMessage(), e);
    } finally {
      exitOperation();
    }
  }

  @Scheduled(
      fixedDelayString = "${auto-trading.operation.monitor-fixed-delay-ms:60000}",
      initialDelayString = "${auto-trading.operation.monitor-initial-delay-ms:30000}")
  public void monitorActivePaperAuto() {
    if (!operationEnabled) {
      return;
    }
    findActiveSession()
        .ifPresent(
            session -> {
              try {
                if (isStartingWithoutAiSession(session)) {
                  audit(
                      "monitor",
                      "SKIPPED",
                      "starting_without_ai_session",
                      Map.of("sessionId", session.getSessionId()));
                  log.info(
                      "[AutoTradingOperation] monitor skipped: sessionId={} is STARTING without aiSessionId",
                      session.getSessionId());
                  return;
                }
                AutoTradingResDTO.AiStatus status =
                    autoTradingQueryService.findAiStatus(
                        session.getUserId(), session.getSessionId());
                log.info(
                    "[AutoTradingOperation] monitor: sessionId={}, sessionStatus={}, aiStatus={}, completedCycles={}/{}",
                    session.getSessionId(),
                    status.getSessionStatus(),
                    status.getStatus(),
                    status.getCompletedCycles(),
                    status.getTotalCycles());
              } catch (RuntimeException e) {
                audit(
                    "monitor",
                    "ERROR",
                    e.getMessage(),
                    Map.of("sessionId", session.getSessionId()));
                log.warn("[AutoTradingOperation] monitor failed: {}", e.getMessage(), e);
              }
            });
  }

  @Scheduled(cron = "${auto-trading.operation.stop-cron:0 30 15 * * MON-FRI}", zone = "Asia/Seoul")
  public void stopDailyPaperAuto() {
    if (!operationEnabled) {
      return;
    }
    if (!tryEnterOperation("daily_stop")) {
      return;
    }
    LocalDateTime now = nowKst();
    try {
      if (!isTradingDay(now.toLocalDate())) {
        audit("daily_stop", "SKIPPED", "non_trading_day", Map.of("date", now.toLocalDate()));
        log.info("[AutoTradingOperation] stop skipped: non-trading day {}", now.toLocalDate());
        return;
      }
      findActiveSession()
          .ifPresentOrElse(
              session -> {
                try {
                  AutoTradingResDTO.Session stopped =
                      withRetry(
                          "daily_stop",
                          () ->
                              autoTradingCommandService.stopSession(
                                  session.getUserId(), session.getSessionId()));
                  audit(
                      "daily_stop",
                      "REQUESTED",
                      "",
                      Map.of(
                          "sessionId",
                          stopped.getSessionId(),
                          "status",
                          String.valueOf(stopped.getStatus())));
                  log.info(
                      "[AutoTradingOperation] paper-auto stop requested: sessionId={}, status={}",
                      stopped.getSessionId(),
                      stopped.getStatus());
                } catch (RuntimeException e) {
                  audit(
                      "daily_stop",
                      "ERROR",
                      e.getMessage(),
                      Map.of("sessionId", session.getSessionId()));
                  log.warn("[AutoTradingOperation] paper-auto stop failed: {}", e.getMessage(), e);
                }
              },
              () -> {
                audit("daily_stop", "SKIPPED", "no_active_session", Map.of());
                log.info("[AutoTradingOperation] stop skipped: no active paper-auto session");
              });
    } finally {
      exitOperation();
    }
  }

  boolean isTradingDay(LocalDate date) {
    DayOfWeek day = date.getDayOfWeek();
    return day != DayOfWeek.SATURDAY
        && day != DayOfWeek.SUNDAY
        && !parsedMarketHolidays().holidays().contains(date);
  }

  private Optional<AutoTradingSession> findActiveSession() {
    return autoTradingSessionRepository.findByActiveSlot(ACTIVE_SLOT);
  }

  private Optional<User> findOperator() {
    if (operatorEmail == null || operatorEmail.isBlank()) {
      log.warn("[AutoTradingOperation] operator email is not configured");
      return Optional.empty();
    }
    Optional<User> user = userRepository.findByEmail(operatorEmail.trim());
    if (user.isEmpty()) {
      log.warn("[AutoTradingOperation] operator user not found: {}", operatorEmail.trim());
    }
    return user;
  }

  private Optional<String> validateOperationConfig(boolean requireOperator) {
    if (!hasText(bundleId)) {
      return Optional.of("paper_bundle_id_missing");
    }
    if (requireOperator && !hasText(operatorEmail)) {
      return Optional.of("operator_email_missing");
    }
    if (purchaseOptionId < 1 || purchaseOptionId > 4) {
      return Optional.of("invalid_purchase_option_id");
    }
    if (cycles < 1) {
      return Optional.of("invalid_cycles");
    }
    if (intervalSec < 1) {
      return Optional.of("invalid_interval_sec");
    }
    if (retryAttempts < 1) {
      return Optional.of("invalid_retry_attempts");
    }
    if (retryBackoffMs < 0) {
      return Optional.of("invalid_retry_backoff_ms");
    }
    return Optional.empty();
  }

  private boolean tryEnterOperation(String action) {
    if (operationLock.compareAndSet(false, true)) {
      return true;
    }
    audit(action, "SKIPPED", "operation_lock_busy", Map.of());
    log.warn("[AutoTradingOperation] {} skipped: operation lock busy", action);
    return false;
  }

  private void exitOperation() {
    operationLock.set(false);
  }

  private <T> T withRetry(String action, Supplier<T> supplier) {
    int attempts = Math.max(1, retryAttempts);
    RuntimeException lastError = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return supplier.get();
      } catch (RuntimeException e) {
        lastError = e;
        if (attempt >= attempts) {
          break;
        }
        audit(action, "RETRY", e.getMessage(), Map.of("attempt", attempt, "maxAttempts", attempts));
        sleepBeforeRetry(action, attempt, e);
      }
    }
    throw lastError;
  }

  private void sleepBeforeRetry(String action, int attempt, RuntimeException cause) {
    if (retryBackoffMs <= 0) {
      return;
    }
    try {
      Thread.sleep(retryBackoffMs);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted during auto-trading scheduler retry for "
              + action
              + " after attempt "
              + attempt,
          cause);
    }
  }

  private void audit(String action, String status, String reason, Map<String, ?> details) {
    try {
      LocalDateTime now = nowKst();
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("source", "be_auto_trading_operation_scheduler");
      payload.put("action", safeText(action));
      payload.put("status", safeText(status));
      payload.put("reason", safeText(reason));
      payload.put("dryRun", dryRun);
      payload.put("operationEnabled", operationEnabled);
      payload.put("bundleId", safeText(bundleId));
      payload.put("details", details == null ? Map.of() : details);
      String payloadJson = objectMapper.writeValueAsString(payload);
      autoTradingEventRepository.saveAndFlush(
          AutoTradingEvent.builder()
              .syntheticEventId("scheduler-" + UUID.randomUUID())
              .eventType(AutoTradingEventType.SCHEDULER_AUDIT)
              .bundleId(safeText(bundleId))
              .messageKey(safeText(action) + ":" + safeText(status))
              .payloadJson(payloadJson)
              .rawEventJson(payloadJson)
              .occurredAt(now)
              .receivedAt(now)
              .build());
    } catch (JsonProcessingException e) {
      log.warn(
          "[AutoTradingOperation] scheduler audit serialization failed: {}", e.getMessage(), e);
    } catch (RuntimeException e) {
      log.warn("[AutoTradingOperation] scheduler audit persistence failed: {}", e.getMessage(), e);
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String safeText(String value) {
    return value == null ? "" : value.trim();
  }

  private LocalDateTime nowKst() {
    return ZonedDateTime.now(clock).withZoneSameInstant(KOREA_ZONE).toLocalDateTime();
  }

  private ParsedMarketHolidays parsedMarketHolidays() {
    ParsedMarketHolidays cached = parsedMarketHolidays;
    if (cached != null && cached.matches(marketHolidays)) {
      return cached;
    }
    ParsedMarketHolidays parsed =
        new ParsedMarketHolidays(marketHolidays, parseMarketHolidays(marketHolidays));
    parsedMarketHolidays = parsed;
    return parsed;
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
      throw new IllegalStateException("invalid_market_holiday_config:" + raw, e);
    }
  }

  private static boolean isStartingWithoutAiSession(AutoTradingSession session) {
    return session.getStatus() == AutoTradingSessionStatus.STARTING
        && (session.getAiSessionId() == null || session.getAiSessionId().isBlank());
  }

  private record ParsedMarketHolidays(String raw, Set<LocalDate> holidays) {

    private boolean matches(String marketHolidays) {
      return Objects.equals(raw, marketHolidays);
    }
  }
}
