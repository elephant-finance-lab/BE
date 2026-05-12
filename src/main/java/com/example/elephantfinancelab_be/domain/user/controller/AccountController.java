package com.example.elephantfinancelab_be.domain.user.controller;

import com.example.elephantfinancelab_be.domain.user.dto.req.UserReqDTO;
import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.domain.user.service.command.AccountCommandService;
import com.example.elephantfinancelab_be.domain.user.service.query.AccountQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "증권 계좌 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AccountController {

  private final AccountQueryService accountQueryService;
  private final AccountCommandService accountCommandService;
  private final UserRepository userRepository;

  private Long resolveUserId(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }

  @Operation(summary = "연결된 계좌 목록 조회", description = "로그인한 사용자의 연결된 증권 계좌 목록을 조회합니다.")
  @GetMapping("/users/accounts")
  public ResponseEntity<ApiResponse<List<UserResDTO.AccountInfo>>> getAccountList(
      @AuthenticationPrincipal String email) {
    List<UserResDTO.AccountInfo> result = accountQueryService.findAccountList(resolveUserId(email));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "계좌 등록", description = "증권 계좌를 등록합니다.")
  @PostMapping("/users/accounts")
  public ResponseEntity<ApiResponse<UserResDTO.AccountId>> createAccount(
      @AuthenticationPrincipal String email, @Valid @RequestBody UserReqDTO.CreateAccount request) {
    UserResDTO.AccountId result = accountCommandService.saveAccount(resolveUserId(email), request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "계좌 삭제", description = "계좌를 삭제합니다.")
  @DeleteMapping("/users/accounts/{accountId}")
  public ResponseEntity<ApiResponse<Void>> deleteAccount(
      @AuthenticationPrincipal String email, @PathVariable Long accountId) {
    accountCommandService.deleteAccount(resolveUserId(email), accountId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK));
  }
}
