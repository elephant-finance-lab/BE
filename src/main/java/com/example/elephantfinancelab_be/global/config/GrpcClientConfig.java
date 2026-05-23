package com.example.elephantfinancelab_be.global.config;

import com.elephant.ai.v1.AiBeBridgeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

  @Value("${ai.server.host}")
  private String aiServerHost;

  @Value("${ai.server.port}")
  private int aiServerPort;

  @Bean
  public ManagedChannel aiManagedChannel() {
    return ManagedChannelBuilder.forAddress(aiServerHost, aiServerPort).usePlaintext().build();
  }

  @Bean
  public AiBeBridgeServiceGrpc.AiBeBridgeServiceBlockingStub aiBeBridgeServiceBlockingStub(
      ManagedChannel aiManagedChannel) {
    return AiBeBridgeServiceGrpc.newBlockingStub(aiManagedChannel);
  }
}
