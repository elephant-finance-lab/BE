package com.example.elephantfinancelab_be.domain.chart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.global.config.KisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class KisApprovalKeyClientTest {

  @Test
  @SuppressWarnings("unchecked")
  void reuseIssuedApprovalKeyAcrossWebSocketClients() throws Exception {
    KisProperties properties = new KisProperties();
    properties.setBaseUrl("https://openapi.koreainvestment.com:9443");
    properties.setAppKey("app-key");
    properties.setAppSecret("app-secret");
    HttpClient httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"approval_key\":\"shared-approval-key\"}");
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    KisApprovalKeyClient client =
        new KisApprovalKeyClient(properties, new ObjectMapper(), httpClient);

    String marketIndexKey = client.issueApprovalKey();
    String stockPriceKey = client.issueApprovalKey();

    assertThat(marketIndexKey).isEqualTo("shared-approval-key");
    assertThat(stockPriceKey).isEqualTo(marketIndexKey);
    verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
  }
}
