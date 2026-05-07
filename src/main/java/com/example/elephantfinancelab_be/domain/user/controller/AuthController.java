package com.example.elephantfinancelab_be.domain.user.controller;

import com.example.elephantfinancelab_be.domain.user.converter.UserConverter;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.AuthErrorCode;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import com.example.elephantfinancelab_be.global.util.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final JwtProvider jwtProvider;

  @Operation(summary = "내 정보 조회", description = "토큰 기반으로 현재 로그인한 사용자 정보를 조회합니다.")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResDTO.MeRes>> getMe(
      @AuthenticationPrincipal String email, @RequestParam(required = false) String token) {

    if (token != null) {
      if (!jwtProvider.validateToken(token)) {
        return ResponseEntity.status(AuthErrorCode.TOKEN_INVALID.getStatus())
            .body(ApiResponse.onFailure(AuthErrorCode.TOKEN_INVALID, null));
      }
      String userIdFromToken = jwtProvider.getUserId(token);
      return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
          .body(
              ApiResponse.of(GeneralSuccessCode.OK, UserConverter.toMeRes(userIdFromToken, token)));
    }

    if (email == null) {
      return ResponseEntity.status(AuthErrorCode.TOKEN_MISSING_OR_EXPIRED.getStatus())
          .body(ApiResponse.onFailure(AuthErrorCode.TOKEN_MISSING_OR_EXPIRED, null));
    }

    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, UserConverter.toMeRes(email)));
  }

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
  @PostMapping("/token")
  public ResponseEntity<ApiResponse<UserResDTO.TokenRes>> reissueToken(
      @CookieValue(name = "refreshToken", required = false) String refreshToken,
      HttpServletResponse response) {

    if (refreshToken == null || refreshToken.isBlank()) {
      return ResponseEntity.status(AuthErrorCode.TOKEN_MISSING.getStatus())
          .body(ApiResponse.onFailure(AuthErrorCode.TOKEN_MISSING, null));
    }

    if (!jwtProvider.validateToken(refreshToken)) {
      return ResponseEntity.status(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getStatus())
          .body(ApiResponse.onFailure(AuthErrorCode.REFRESH_TOKEN_EXPIRED, null));
    }

    String userId = jwtProvider.getUserId(refreshToken);

    if (!jwtProvider.validateRefreshToken(userId, refreshToken)) {
      return ResponseEntity.status(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getStatus())
          .body(ApiResponse.onFailure(AuthErrorCode.REFRESH_TOKEN_EXPIRED, null));
    }

    String newAccessToken = jwtProvider.generateAccessToken(userId);
    response.setHeader("Authorization", "Bearer " + newAccessToken);

    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, UserConverter.toTokenRes(newAccessToken)));
  }

  @Operation(
      summary = "구글 로그인",
      description =
          "구글 소셜 로그인 페이지로 리다이렉트합니다. 브라우저에서 직접 접속하세요: http://localhost:8080/oauth2/authorization/google")
  @GetMapping("/login/google")
  public ResponseEntity<ApiResponse<Void>> loginGoogle() {
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }

  @Operation(
      summary = "네이버 로그인",
      description =
          "네이버 소셜 로그인 페이지로 리다이렉트합니다. 브라우저에서 직접 접속하세요: http://localhost:8080/oauth2/authorization/naver")
  @GetMapping("/login/naver")
  public ResponseEntity<ApiResponse<Void>> loginNaver() {
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }

  @Operation(
      summary = "카카오 로그인",
      description =
          "카카오 소셜 로그인 페이지로 리다이렉트합니다. 브라우저에서 직접 접속하세요: http://localhost:8080/oauth2/authorization/kakao")
  @GetMapping("/login/kakao")
  public ResponseEntity<ApiResponse<Void>> loginKakao() {
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }
}
