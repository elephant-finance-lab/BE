package com.example.elephantfinancelab_be.domain.autotrading.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;
import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingSessionStatus;
import com.example.elephantfinancelab_be.domain.autotrading.service.command.AutoTradingCommandService;
import com.example.elephantfinancelab_be.domain.autotrading.service.query.AutoTradingQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AutoTradingControllerTest {

  private final AutoTradingCommandService commandService = mock(AutoTradingCommandService.class);
  private final AutoTradingQueryService queryService = mock(AutoTradingQueryService.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AutoTradingController controller =
      new AutoTradingController(commandService, queryService, userRepository);

  @Test
  void activeEndpointReturnsCurrentUserSession() {
    when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(1L).email("user@example.com").build()));
    when(queryService.findActiveSession(1L))
        .thenReturn(
            AutoTradingResDTO.Session.builder()
                .sessionId("be-session-active")
                .status(AutoTradingSessionStatus.RUNNING)
                .build());

    ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> response =
        controller.getActiveSession("user@example.com");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getResult().getSessionId()).isEqualTo("be-session-active");
  }

  @Test
  void activeEndpointReturnsNullResultWhenNoSessionExists() {
    when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(1L).email("user@example.com").build()));
    when(queryService.findActiveSession(1L)).thenReturn(null);

    ResponseEntity<ApiResponse<AutoTradingResDTO.Session>> response =
        controller.getActiveSession("user@example.com");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getResult()).isNull();
  }

  @Test
  void readinessEndpointReturnsAiReadinessShape() {
    when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(1L).email("user@example.com").build()));
    when(queryService.findReadiness(1L, "BUNDLE-TEST"))
        .thenReturn(
            AutoTradingResDTO.Readiness.builder()
                .status("PASS")
                .bundleId("BUNDLE-TEST")
                .deployQuality("PASS")
                .brokerEvidence("PASS")
                .liveTradingAllowed(false)
                .registryMutated(false)
                .safeToEnableOrderActions(true)
                .safeToEnableLiveActions(false)
                .activeSessionExists(false)
                .activeSessionOwnedByCurrentUser(false)
                .canStartPaperAutoTrading(true)
                .build());

    ResponseEntity<ApiResponse<AutoTradingResDTO.Readiness>> response =
        controller.getReadiness("user@example.com", "BUNDLE-TEST");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getResult().isCanStartPaperAutoTrading()).isTrue();
    assertThat(response.getBody().getResult().isRegistryMutated()).isFalse();
  }
}
