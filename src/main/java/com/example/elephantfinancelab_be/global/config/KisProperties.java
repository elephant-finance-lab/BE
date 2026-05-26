package com.example.elephantfinancelab_be.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

  private static final String REAL_BASE_URL = "https://openapi.koreainvestment.com:9443";
  private static final String VIRTUAL_BASE_URL = "https://openapivts.koreainvestment.com:29443";
  private static final String REAL_WEBSOCKET_URL = "ws://ops.koreainvestment.com:21000";
  private static final String VIRTUAL_WEBSOCKET_URL = "ws://ops.koreainvestment.com:31000";

  private Mode mode = Mode.REAL;
  @NotBlank private String appKey;
  @NotBlank private String appSecret;
  private String baseUrl;
  private String websocketUrl;
  private String financialAppKey;
  private String financialAppSecret;
  private String financialBaseUrl;
  private boolean websocketEnabled = true;

  public String getBaseUrl() {
    return hasText(baseUrl) ? baseUrl : mode.baseUrl();
  }

  public String getWebsocketUrl() {
    return hasText(websocketUrl) ? websocketUrl : mode.websocketUrl();
  }

  public String getFinancialAppKeyOrDefault() {
    return hasText(financialAppKey) ? financialAppKey : appKey;
  }

  public String getFinancialAppSecretOrDefault() {
    return hasText(financialAppSecret) ? financialAppSecret : appSecret;
  }

  public String getFinancialBaseUrlOrDefault() {
    return hasText(financialBaseUrl) ? financialBaseUrl : getBaseUrl();
  }

  @PostConstruct
  void validateOfficialEndpointMatchesMode() {
    validateModeEndpoint("base-url", getBaseUrl(), mode.baseUrl(), mode.other().baseUrl());
    validateModeEndpoint(
        "financial-base-url",
        getFinancialBaseUrlOrDefault(),
        mode.baseUrl(),
        mode.other().baseUrl());
    validateModeEndpoint(
        "websocket-url", getWebsocketUrl(), mode.websocketUrl(), mode.other().websocketUrl());
  }

  private void validateModeEndpoint(
      String propertyName, String configuredUrl, String expectedUrl, String conflictingUrl) {
    if (normalizedUrl(configuredUrl).equals(normalizedUrl(conflictingUrl))) {
      throw new IllegalStateException(
          "kis."
              + propertyName
              + " does not match kis.mode="
              + mode.name().toLowerCase(Locale.ROOT)
              + ". Use "
              + expectedUrl
              + " or remove the explicit URL override.");
    }
  }

  private String normalizedUrl(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public enum Mode {
    REAL(REAL_BASE_URL, REAL_WEBSOCKET_URL),
    VIRTUAL(VIRTUAL_BASE_URL, VIRTUAL_WEBSOCKET_URL);

    private final String baseUrl;
    private final String websocketUrl;

    Mode(String baseUrl, String websocketUrl) {
      this.baseUrl = baseUrl;
      this.websocketUrl = websocketUrl;
    }

    String baseUrl() {
      return baseUrl;
    }

    String websocketUrl() {
      return websocketUrl;
    }

    Mode other() {
      return this == REAL ? VIRTUAL : REAL;
    }
  }
}
