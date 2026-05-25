package com.example.elephantfinancelab_be.global.security.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CustomLogoutSuccessHandlerTest {

  @Test
  void logoutRevokesRefreshTokenWhenAccessTokenIsExpiredButSigned() throws Exception {
    JwtProvider jwtProvider = mock(JwtProvider.class);
    CustomLogoutSuccessHandler handler = new CustomLogoutSuccessHandler(jwtProvider);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer expired-access-token");
    when(jwtProvider.getUserIdAllowExpired("expired-access-token"))
        .thenReturn(Optional.of("user@example.com"));

    handler.onLogoutSuccess(request, response, null);

    verify(jwtProvider).deleteRefreshToken("user@example.com");
  }
}
