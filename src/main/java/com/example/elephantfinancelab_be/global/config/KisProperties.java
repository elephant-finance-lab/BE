package com.example.elephantfinancelab_be.global.config;

import jakarta.validation.constraints.NotBlank;
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

  @NotBlank private String appKey;
  @NotBlank private String appSecret;
  @NotBlank private String baseUrl;
  @NotBlank private String websocketUrl;
  private String financialAppKey;
  private String financialAppSecret;
  private boolean websocketEnabled = true;

  public String getFinancialAppKeyOrDefault() {
    return hasText(financialAppKey) ? financialAppKey : appKey;
  }

  public String getFinancialAppSecretOrDefault() {
    return hasText(financialAppSecret) ? financialAppSecret : appSecret;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
