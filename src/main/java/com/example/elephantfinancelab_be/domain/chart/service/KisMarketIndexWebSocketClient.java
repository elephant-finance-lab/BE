package com.example.elephantfinancelab_be.domain.chart.service;

import com.example.elephantfinancelab_be.domain.chart.entity.MarketIndexMarket;
import com.example.elephantfinancelab_be.domain.chart.exception.code.ChartErrorCode;
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
  private final HttpClient httpClient;
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
      log.info("한국투자증권 시장 지수 웹소켓이 비활성화되어 있습니다.");
      return;
    }

    if (!hasText(kisProperties.getAppKey()) || !hasText(kisProperties.getAppSecret())) {
      log.warn(
          "code={}, message={}, reason=missing-kis-credentials",
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage());
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
      currentWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "서버 종료");
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
                  log.warn(
                      "code={}, message={}",
                      ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
                      ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
                      throwable);
                  scheduleReconnect();
                  return;
                }
                webSocket = socket;
              });
    } catch (RuntimeException e) {
      log.warn(
          "code={}, message={}, phase=prepare-connect",
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
          e);
      scheduleReconnect();
    }
  }

  private void scheduleReconnect() {
    if (!running.get() || !reconnectScheduled.compareAndSet(false, true)) {
      return;
    }

    log.info("한국투자증권 시장 지수 웹소켓 재연결을 {}초 후 시도합니다.", RECONNECT_DELAY.toSeconds());
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
                      "code={}, message={}, market={}",
                      ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
                      ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
                      market.name(),
                      throwable);
                  return;
                }
                log.info("한국투자증권 시장 지수 구독 완료. market={}", market.name());
              });
    } catch (JsonProcessingException e) {
      log.warn(
          "code={}, message={}, market={}",
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
          market.name(),
          e);
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
                  "한국투자증권 시장 지수 메시지 수신. market={}, value={}",
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
          "한국투자증권 시장 지수 웹소켓 응답. tr_id={}, tr_key={}, rt_cd={}, msg_cd={}",
          header.path("tr_id").asText(),
          header.path("tr_key").asText(),
          body.path("rt_cd").asText(),
          body.path("msg_cd").asText());
    } catch (JsonProcessingException e) {
      log.debug("한국투자증권 시장 지수 웹소켓 비데이터 메시지를 수신했습니다.");
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
      log.info("한국투자증권 시장 지수 웹소켓 연결 완료");
      WebSocket.Listener.super.onOpen(socket);
      for (MarketIndexMarket market : MarketIndexMarket.values()) {
        subscribe(socket, approvalKey, market);
      }
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
      textBuffer.append(data);
      if (last) {
        try {
          handleMessage(textBuffer.toString());
        } catch (RuntimeException e) {
          log.warn(
              "code={}, message={}, phase=handle-message",
              ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
              ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
              e);
        } finally {
          textBuffer.setLength(0);
        }
      }
      socket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
      log.warn(
          "code={}, message={}, statusCode={}, reason={}",
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
          statusCode,
          reason);
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
      log.warn(
          "code={}, message={}",
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getCode(),
          ChartErrorCode.KIS_MARKET_INDEX_WEBSOCKET_FAILED.getMessage(),
          error);
      scheduleReconnect();
    }
  }
}
