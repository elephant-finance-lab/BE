package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisMarketIndexWebSocketClient {

  private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final KisApprovalKeyClient approvalKeyClient;
  private final MarketIndexRealtimeParser marketIndexRealtimeParser;
  private final MarketIndexRedisService marketIndexRedisService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final ScheduledExecutorService reconnectExecutor =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "kis-market-index-ws-reconnect");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

  private volatile WebSocket webSocket;

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (!kisProperties.isWebsocketEnabled()) {
      log.info("KIS market index WebSocket is disabled");
      return;
    }

    if (!hasText(kisProperties.getAppKey()) || !hasText(kisProperties.getAppSecret())) {
      log.warn("KIS market index WebSocket skipped because credentials are not configured");
      return;
    }

    running.set(true);
    connect();
  }

  @PreDestroy
  public void stop() {
    running.set(false);
    WebSocket currentWebSocket = webSocket;
    if (currentWebSocket != null) {
      currentWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "server shutdown");
    }
    reconnectExecutor.shutdownNow();
  }

  private void connect() {
    if (!running.get()) {
      return;
    }

    try {
      String approvalKey = approvalKeyClient.issueApprovalKey();
      httpClient
          .newWebSocketBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .buildAsync(
              URI.create(kisProperties.getWebsocketUrl()), new KisWebSocketListener(approvalKey))
          .whenComplete(
              (socket, throwable) -> {
                if (throwable != null) {
                  log.warn("KIS market index WebSocket connection failed", throwable);
                  scheduleReconnect();
                  return;
                }
                webSocket = socket;
              });
    } catch (RuntimeException e) {
      log.warn("KIS market index WebSocket connection preparation failed", e);
      scheduleReconnect();
    }
  }

  private void scheduleReconnect() {
    if (!running.get() || !reconnectScheduled.compareAndSet(false, true)) {
      return;
    }

    log.info(
        "KIS market index WebSocket reconnect scheduled after {} seconds",
        RECONNECT_DELAY.toSeconds());
    reconnectExecutor.schedule(
        () -> {
          reconnectScheduled.set(false);
          connect();
        },
        RECONNECT_DELAY.toSeconds(),
        TimeUnit.SECONDS);
  }

  private void subscribe(WebSocket socket, String approvalKey, MarketIndexMarket market) {
    try {
      socket
          .sendText(subscriptionMessage(approvalKey, market), true)
          .whenComplete(
              (unused, throwable) -> {
                if (throwable != null) {
                  log.warn(
                      "KIS market index subscription failed. market={}", market.name(), throwable);
                  return;
                }
                log.info("KIS market index subscribed. market={}", market.name());
              });
    } catch (JsonProcessingException e) {
      log.warn(
          "Failed to build KIS market index subscription message. market={}", market.name(), e);
    }
  }

  private String subscriptionMessage(String approvalKey, MarketIndexMarket market)
      throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        Map.of(
            "header",
            Map.of(
                "approval_key",
                approvalKey,
                "custtype",
                "P",
                "tr_type",
                "1",
                "content-type",
                "utf-8"),
            "body",
            Map.of(
                "input",
                Map.of(
                    "tr_id",
                    MarketIndexRealtimeParser.DOMESTIC_INDEX_REALTIME_TR_ID,
                    "tr_key",
                    market.getKisIndexCode()))));
  }

  private void handleMessage(String message) {
    if (message.startsWith("{")) {
      logSubscriptionResponse(message);
      return;
    }

    marketIndexRealtimeParser
        .parseAll(message)
        .forEach(
            parsed -> {
              marketIndexRedisService.save(parsed.market(), parsed.index());
              log.debug(
                  "KIS market index message received. market={}, value={}",
                  parsed.market().name(),
                  parsed.index().value());
            });
  }

  private void logSubscriptionResponse(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      JsonNode header = root.path("header");
      JsonNode body = root.path("body");
      log.info(
          "KIS market index WebSocket response. tr_id={}, tr_key={}, rt_cd={}, msg_cd={}",
          header.path("tr_id").asText(),
          header.path("tr_key").asText(),
          body.path("rt_cd").asText(),
          body.path("msg_cd").asText());
    } catch (JsonProcessingException e) {
      log.debug("KIS market index WebSocket non-data message received");
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private class KisWebSocketListener implements WebSocket.Listener {

    private final String approvalKey;
    private final StringBuilder textBuffer = new StringBuilder();

    private KisWebSocketListener(String approvalKey) {
      this.approvalKey = approvalKey;
    }

    @Override
    public void onOpen(WebSocket socket) {
      log.info("KIS market index WebSocket connected");
      WebSocket.Listener.super.onOpen(socket);
      for (MarketIndexMarket market : MarketIndexMarket.values()) {
        subscribe(socket, approvalKey, market);
      }
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
      textBuffer.append(data);
      if (last) {
        handleMessage(textBuffer.toString());
        textBuffer.setLength(0);
      }
      socket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
      log.warn("KIS market index WebSocket closed. statusCode={}, reason={}", statusCode, reason);
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
      log.warn("KIS market index WebSocket error", error);
      scheduleReconnect();
    }
  }
}
