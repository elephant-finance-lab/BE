package com.example.elephantfinancelab_be.domain.user.controller;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.service.command.UserCommandService;
import com.example.elephantfinancelab_be.domain.user.service.query.UserQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private static final Long DEV_USER_ID = 1L;

  private final UserQueryService userQueryService;
  private final UserCommandService userCommandService;

  @Operation(summary = "내 프로필 조회", description = "이름, 전화번호, 성별, 프로필 이미지 URL을 조회합니다.")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResDTO.Profile>> getMyProfile() {
    UserResDTO.Profile result = userQueryService.getMyProfile(DEV_USER_ID);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "내 정보 수정", description = "이름과 전화번호를 수정합니다.")
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<Void>> updateMyProfile(
      @Valid @RequestBody UserReqDTO.UpdateProfile request) {
    userCommandService.updateProfile(DEV_USER_ID, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }

  @Operation(summary = "회원 탈퇴", description = "소프트 삭제 처리합니다.")
  @PostMapping("/me/withdraw")
  public ResponseEntity<ApiResponse<Void>> withdraw() {
    userCommandService.withdraw(DEV_USER_ID);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }
}
