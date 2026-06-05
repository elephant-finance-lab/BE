package com.example.elephantfinancelab_be.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.elephantfinancelab_be.global.config.KisProperties.Mode;
import org.junit.jupiter.api.Test;

class KisPropertiesTest {

  @Test
  void defaultsMarketDataEndpointsToRealEnvironment() {
    KisProperties properties = new KisProperties();

    assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.koreainvestment.com:9443");
    assertThat(properties.getWebsocketUrl()).isEqualTo("ws://ops.koreainvestment.com:21000");
  }

  @Test
  void selectsVirtualRestAndWebsocketEndpointsFromMode() {
    KisProperties properties = new KisProperties();
    properties.setMode(Mode.VIRTUAL);

    assertThat(properties.getBaseUrl()).isEqualTo("https://openapivts.koreainvestment.com:29443");
    assertThat(properties.getWebsocketUrl()).isEqualTo("ws://ops.koreainvestment.com:31000");
  }

  @Test
  void acceptsAnExplicitRealFinancialEndpointInRealMode() {
    KisProperties properties = new KisProperties();
    properties.setFinancialBaseUrl("https://openapi.koreainvestment.com:9443");

    properties.validateOfficialEndpointMatchesMode();

    assertThat(properties.getBaseUrl()).isEqualTo("https://openapi.koreainvestment.com:9443");
    assertThat(properties.getFinancialBaseUrlOrDefault())
        .isEqualTo("https://openapi.koreainvestment.com:9443");
  }

  @Test
  void allowsRealFinancialEndpointInVirtualModeForPaperPortfolioAndMarketDataSplit() {
    KisProperties properties = new KisProperties();
    properties.setMode(Mode.VIRTUAL);
    properties.setFinancialBaseUrl("https://openapi.koreainvestment.com:9443");

    properties.validateOfficialEndpointMatchesMode();

    assertThat(properties.getBaseUrl()).isEqualTo("https://openapivts.koreainvestment.com:29443");
    assertThat(properties.getFinancialBaseUrlOrDefault())
        .isEqualTo("https://openapi.koreainvestment.com:9443");
  }

  @Test
  void rejectsAnExplicitRealRestEndpointInVirtualMode() {
    KisProperties properties = new KisProperties();
    properties.setMode(Mode.VIRTUAL);
    properties.setBaseUrl("https://openapi.koreainvestment.com:9443");

    assertThatThrownBy(properties::validateOfficialEndpointMatchesMode)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("kis.base-url does not match kis.mode=virtual");
  }

  @Test
  void allowsVirtualFinancialEndpointInRealModeForExplicitLocalOverrides() {
    KisProperties properties = new KisProperties();
    properties.setFinancialBaseUrl("https://openapivts.koreainvestment.com:29443");

    properties.validateOfficialEndpointMatchesMode();

    assertThat(properties.getFinancialBaseUrlOrDefault())
        .isEqualTo("https://openapivts.koreainvestment.com:29443");
  }
}
