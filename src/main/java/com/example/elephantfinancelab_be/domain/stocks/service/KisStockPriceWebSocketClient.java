package com.example.elephantfinancelab_be.domain.stocks.service;

import com.example.elephantfinancelab_be.domain.chart.service.KisApprovalKeyClient;
import com.example.elephantfinancelab_be.domain.stocks.exception.code.StockErrorCode;
import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
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
public class KisStockPriceWebSocketClient {

  private static final Duration RECONNECT_DELAY = Duration.ofSeconds(10);

  private final KisProperties kisProperties;
  private final KisApprovalKeyClient approvalKeyClient;
  private final StockPriceRealtimeParser stockPriceRealtimeParser;
  private final StockPricePushService stockPricePushService;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Set<String> subscribedTickers = ConcurrentHashMap.newKeySet();
  private final ScheduledExecutorService reconnectExecutor =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "kis-stock-price-ws-reconnect");
            thread.setDaemon(true);
            return thread;
          });
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicBoolean connecting = new AtomicBoolean(false);
  private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

  private volatile String approvalKey;
  private volatile WebSocket webSocket;

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    if (!kisProperties.isWebsocketEnabled()) {
      log.info("한국투자증권 종목 체결가 웹소켓이 비활성화되어 있습니다.");
      return;
    }

    if (!hasText(kisProperties.getAppKey()) || !hasText(kisProperties.getAppSecret())) {
      log.warn(
          "code={}, message={}, reason=missing-kis-credentials",
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage());
      return;
    }

    running.set(true);
    connect();
  }

  public void subscribe(String ticker) {
    String normalizedTicker = normalizeTicker(ticker);
    if (normalizedTicker == null) {
      return;
    }

    boolean added = subscribedTickers.add(normalizedTicker);
    WebSocket currentWebSocket = webSocket;
    String currentApprovalKey = approvalKey;
    if (added && currentWebSocket != null && currentApprovalKey != null) {
      subscribe(currentWebSocket, currentApprovalKey, normalizedTicker);
    }
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
    if (!running.get() || !connecting.compareAndSet(false, true)) {
      return;
    }

    try {
      String issuedApprovalKey = approvalKeyClient.issueApprovalKey();
      httpClient
          .newWebSocketBuilder()
          .connectTimeout(Duration.ofSeconds(10))
          .buildAsync(
              URI.create(kisProperties.getWebsocketUrl()),
              new KisWebSocketListener(issuedApprovalKey))
          .whenComplete(
              (socket, throwable) -> {
                connecting.set(false);
                if (throwable != null) {
                  log.warn(
                      "code={}, message={}",
                      StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
                      StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
                      throwable);
                  scheduleReconnect();
                  return;
                }
                approvalKey = issuedApprovalKey;
                webSocket = socket;
              });
    } catch (RuntimeException e) {
      connecting.set(false);
      log.warn(
          "code={}, message={}, phase=prepare-connect",
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
          e);
      scheduleReconnect();
    }
  }

  private void scheduleReconnect() {
    if (!running.get() || !reconnectScheduled.compareAndSet(false, true)) {
      return;
    }

    log.info("한국투자증권 종목 체결가 웹소켓 재연결을 {}초 후 시도합니다.", RECONNECT_DELAY.toSeconds());
    reconnectExecutor.schedule(
        () -> {
          reconnectScheduled.set(false);
          connect();
        },
        RECONNECT_DELAY.toSeconds(),
        TimeUnit.SECONDS);
  }

  private void subscribe(WebSocket socket, String approvalKey, String ticker) {
    try {
      socket
          .sendText(subscriptionMessage(approvalKey, ticker), true)
          .whenComplete(
              (unused, throwable) -> {
                if (throwable != null) {
                  log.warn(
                      "code={}, message={}, ticker={}",
                      StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
                      StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
                      ticker,
                      throwable);
                  return;
                }
                log.info("한국투자증권 종목 체결가 구독 완료. ticker={}", ticker);
              });
    } catch (JsonProcessingException e) {
      log.warn(
          "code={}, message={}, ticker={}",
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
          ticker,
          e);
    }
  }

  private String subscriptionMessage(String approvalKey, String ticker)
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
                    StockPriceRealtimeParser.DOMESTIC_STOCK_REALTIME_TR_ID,
                    "tr_key",
                    ticker))));
  }

  private void handleMessage(String message) {
    if (message.startsWith("{")) {
      logSubscriptionResponse(message);
      return;
    }

    stockPriceRealtimeParser.parseAll(message).forEach(stockPricePushService::updateAndPush);
  }

  private void logSubscriptionResponse(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      JsonNode header = root.path("header");
      JsonNode body = root.path("body");
      log.info(
          "한국투자증권 종목 체결가 웹소켓 응답. tr_id={}, tr_key={}, rt_cd={}, msg_cd={}",
          header.path("tr_id").asText(),
          header.path("tr_key").asText(),
          body.path("rt_cd").asText(),
          body.path("msg_cd").asText());
    } catch (JsonProcessingException e) {
      log.debug("한국투자증권 종목 체결가 웹소켓 비데이터 메시지를 수신했습니다.");
    }
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      return null;
    }
    return ticker.trim().toUpperCase(Locale.ROOT);
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
      log.info("한국투자증권 종목 체결가 웹소켓 연결 완료");
      WebSocket.Listener.super.onOpen(socket);
      socket.request(1);
      subscribedTickers.forEach(ticker -> subscribe(socket, approvalKey, ticker));
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
              StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
              StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
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
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
          statusCode,
          reason);
      webSocket = null;
      KisStockPriceWebSocketClient.this.approvalKey = null;
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
      log.warn(
          "code={}, message={}",
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getCode(),
          StockErrorCode.KIS_STOCK_PRICE_WEBSOCKET_FAILED.getMessage(),
          error);
      webSocket = null;
      KisStockPriceWebSocketClient.this.approvalKey = null;
      scheduleReconnect();
    }
  }
}
