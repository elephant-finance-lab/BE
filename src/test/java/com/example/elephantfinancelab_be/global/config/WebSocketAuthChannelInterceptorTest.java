package com.example.elephantfinancelab_be.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class WebSocketAuthChannelInterceptorTest {

  private final JwtProvider jwtProvider = mock(JwtProvider.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final WebSocketAuthChannelInterceptor interceptor =
      new WebSocketAuthChannelInterceptor(jwtProvider, userRepository);

  @Test
  void authenticatesConnectWithJwtAndUsesUserIdAsPrincipalName() {
    when(jwtProvider.validateToken("token")).thenReturn(true);
    when(jwtProvider.getUserId("token")).thenReturn("user@example.com");
    when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(User.builder().id(10L).email("user@example.com").build()));

    Message<?> result =
        interceptor.preSend(message(StompCommand.CONNECT, null, "Bearer token"), channel());

    assertThat(StompHeaderAccessor.wrap(result).getUser()).isNotNull();
    assertThat(StompHeaderAccessor.wrap(result).getUser().getName()).isEqualTo("10");
  }

  @Test
  void blocksDirectUserTopicSubscription() {
    assertThatThrownBy(
            () ->
                interceptor.preSend(
                    message(StompCommand.SUBSCRIBE, "/topic/users/10/notifications", null),
                    channel()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void blocksAnonymousUserQueueSubscription() {
    assertThatThrownBy(
            () ->
                interceptor.preSend(
                    message(StompCommand.SUBSCRIBE, "/user/queue/notifications", null), channel()))
        .isInstanceOf(AccessDeniedException.class);
  }

  private static Message<?> message(
      StompCommand command, String destination, String authorization) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    if (destination != null) {
      accessor.setDestination(destination);
    }
    if (authorization != null) {
      accessor.setNativeHeader("Authorization", authorization);
    }
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private static MessageChannel channel() {
    return mock(MessageChannel.class);
  }
}
