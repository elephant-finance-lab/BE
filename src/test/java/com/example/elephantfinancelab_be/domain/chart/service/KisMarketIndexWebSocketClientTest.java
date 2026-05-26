package com.example.elephantfinancelab_be.domain.chart.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class KisMarketIndexWebSocketClientTest {

  @Test
  void echoPingPongMessageToKeepKisSessionAlive() {
    WebSocket socket = mock(WebSocket.class);
    String message = "{\"header\":{\"tr_id\":\"PINGPONG\",\"datetime\":\"20260526133000\"}}";
    when(socket.sendText(message, true)).thenReturn(CompletableFuture.completedFuture(socket));
    KisMarketIndexWebSocketClient client =
        new KisMarketIndexWebSocketClient(
            mock(KisProperties.class),
            mock(KisApprovalKeyClient.class),
            mock(MarketIndexRealtimeParser.class),
            mock(MarketIndexRedisService.class),
            mock(MarketIndexPushService.class),
            new ObjectMapper(),
            mock(HttpClient.class));

    client.handleControlMessage(socket, message);

    verify(socket).sendText(message, true);
    client.stop();
  }
}
