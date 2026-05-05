package com.example.elephantfinancelab_be.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

  private String appKey;
  private String appSecret;
  private String baseUrl;
  private String websocketUrl;
  private boolean websocketEnabled = true;
}
