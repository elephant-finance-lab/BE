package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    if (accessor.getCommand() == StompCommand.CONNECT) {
      authenticate(accessor);
    }
    if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
      authorizeSubscription(accessor);
    }
    return message;
  }

  private void authenticate(StompHeaderAccessor accessor) {
    String token = bearerToken(accessor);
    if (token == null || !jwtProvider.validateToken(token)) {
      return;
    }
    String tokenSubject = jwtProvider.getUserId(token);
    resolveUser(tokenSubject)
        .ifPresent(
            user ->
                accessor.setUser(
                    new UsernamePasswordAuthenticationToken(
                        String.valueOf(user.getId()), null, List.of())));
  }

  private void authorizeSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || destination.isBlank()) {
      return;
    }
    if (destination.startsWith("/topic/users/")) {
      throw new AccessDeniedException("사용자 알림 topic 직접 구독은 허용되지 않습니다.");
    }
    if (destination.startsWith("/user/queue/notifications") && accessor.getUser() == null) {
      throw new AccessDeniedException("알림 구독에는 인증이 필요합니다.");
    }
  }

  private Optional<User> resolveUser(String tokenSubject) {
    if (tokenSubject == null || tokenSubject.isBlank()) {
      return Optional.empty();
    }
    return userRepository
        .findByEmail(tokenSubject)
        .or(() -> userRepository.findByProviderUserId(tokenSubject));
  }

  private static String bearerToken(StompHeaderAccessor accessor) {
    String authorization = accessor.getFirstNativeHeader("Authorization");
    if (authorization != null && authorization.startsWith("Bearer ")) {
      return authorization.substring(7).trim();
    }
    if (authorization != null && !authorization.isBlank()) {
      return authorization.trim();
    }
    String accessToken = accessor.getFirstNativeHeader("access_token");
    return accessToken == null || accessToken.isBlank() ? null : accessToken.trim();
  }
}
