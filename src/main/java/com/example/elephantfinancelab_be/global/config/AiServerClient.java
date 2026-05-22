package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.global.apiPayload.code.AiServerErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.exception.AiServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AiServerClient {

  private final WebClient webClient;

  public AiServerClient(
      WebClient.Builder webClientBuilder, @Value("${ai.server.url}") String aiServerUrl) {
    this.webClient =
        webClientBuilder
            .baseUrl(aiServerUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .filter(
                (request, next) -> {
                  log.debug("[AI Client] {} {}", request.method(), request.url());
                  return next.exchange(request);
                })
            .build();

    log.info("[AI Client] 초기화 완료 - URL: {}", aiServerUrl);
  }

  public <T> Mono<T> get(String path, Class<T> responseType) {
    return webClient
        .get()
        .uri(path)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> Mono.error(new AiServerException(AiServerErrorCode.AI400_01)))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(new AiServerException(AiServerErrorCode.AI500_01)))
        .bodyToMono(responseType)
        .doOnError(e -> log.error("[AI Client] GET {} 실패: {}", path, e.getMessage()))
        .onErrorMap(
            e -> !(e instanceof AiServerException),
            e -> new AiServerException(AiServerErrorCode.AI503_01));
  }

  public <T> Mono<T> post(String path, Object requestBody, Class<T> responseType) {
    return webClient
        .post()
        .uri(path)
        .bodyValue(requestBody)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> Mono.error(new AiServerException(AiServerErrorCode.AI400_01)))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(new AiServerException(AiServerErrorCode.AI500_01)))
        .bodyToMono(responseType)
        .doOnError(e -> log.error("[AI Client] POST {} 실패: {}", path, e.getMessage()))
        .onErrorMap(
            e -> !(e instanceof AiServerException),
            e -> new AiServerException(AiServerErrorCode.AI503_01));
  }

  public Mono<Boolean> healthCheck() {
    return webClient
        .get()
        .uri("/health")
        .retrieve()
        .toBodilessEntity()
        .map(response -> response.getStatusCode().is2xxSuccessful())
        .doOnSuccess(ok -> log.info("[AI Client] 헬스체크: {}", ok ? "정상" : "비정상"))
        .onErrorResume(
            e -> {
              log.warn("[AI Client] 헬스체크 실패: {}", e.getMessage());
              return Mono.just(false);
            });
  }
}
