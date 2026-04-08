package com.example.elephantfinancelab_be.global.auth.controller;

import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.AuthErrorCode;
import com.example.elephantfinancelab_be.global.auth.service.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final JwtProvider jwtProvider;

  public AuthController(JwtProvider jwtProvider) {
    this.jwtProvider = jwtProvider;
  }

  @GetMapping("/me")
  public ApiResponse<?> getMe(
      @AuthenticationPrincipal String userId, @RequestParam(required = false) String token) {

    if (token != null) {
      if (!jwtProvider.validateToken(token)) {
        return ApiResponse.onFailure(AuthErrorCode.TOKEN_INVALID, null);
      }
      String userIdFromToken = jwtProvider.getUserId(token);
      return new ApiResponse<>(
          true,
          "COMMON200",
          "성공입니다.",
          Map.of(
              "userId", userIdFromToken,
              "accessToken", token));
    }

    if (userId == null) return ApiResponse.onFailure(AuthErrorCode.TOKEN_MISSING_OR_EXPIRED, null);

    return new ApiResponse<>(true, "COMMON200", "성공입니다.", Map.of("userId", userId));
  }

  @PostMapping("/token")
  public ApiResponse<?> reissueToken(
      @CookieValue(name = "refreshToken", required = false) String refreshToken,
      HttpServletResponse response) {

    if (refreshToken == null || refreshToken.isBlank()) {
      return ApiResponse.onFailure(AuthErrorCode.TOKEN_MISSING_OR_EXPIRED, null);
    }

    if (!jwtProvider.validateToken(refreshToken)) {
      return ApiResponse.onFailure(AuthErrorCode.TOKEN_INVALID, null);
    }

    String userId = jwtProvider.getUserId(refreshToken);

    if (!jwtProvider.validateRefreshToken(userId, refreshToken)) {
      return ApiResponse.onFailure(AuthErrorCode.TOKEN_MISSING_OR_EXPIRED, null);
    }

    String newAccessToken = jwtProvider.generateAccessToken(userId);
    response.setHeader("Authorization", "Bearer " + newAccessToken);

    return new ApiResponse<>(true, "COMMON200", "성공입니다.", Map.of("accessToken", newAccessToken));
  }
}
