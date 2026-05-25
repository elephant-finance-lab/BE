package com.example.elephantfinancelab_be.global.config;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;

  private Optional<String> resolvePrincipal(String tokenSubject) {
    Optional<User> user =
        userRepository
            .findByEmail(tokenSubject)
            .or(() -> userRepository.findByProviderUserId(tokenSubject));

    return user.map(User::getEmail);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null && jwtProvider.validateToken(token)) {
      String userId = jwtProvider.getUserId(token);
      resolvePrincipal(userId)
          .ifPresent(
              principal -> {
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if (bearer != null && bearer.startsWith("Bearer ")) {
      return bearer.substring(7);
    }
    return null;
  }
}
