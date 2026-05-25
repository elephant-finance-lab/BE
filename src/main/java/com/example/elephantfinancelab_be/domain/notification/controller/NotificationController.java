package com.example.elephantfinancelab_be.domain.notification.controller;

import com.example.elephantfinancelab_be.domain.notification.dto.res.NotificationResDTO;
import com.example.elephantfinancelab_be.domain.notification.service.command.NotificationCommandService;
import com.example.elephantfinancelab_be.domain.notification.service.query.NotificationQueryService;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Notification", description = "사용자 알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationQueryService notificationQueryService;
  private final NotificationCommandService notificationCommandService;
  private final UserRepository userRepository;

  @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림을 최신순으로 조회합니다.")
  @GetMapping
  public ResponseEntity<ApiResponse<NotificationResDTO.Page>> getNotifications(
      @AuthenticationPrincipal String email,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    NotificationResDTO.Page result =
        notificationQueryService.findNotifications(
            resolveUserId(email), PageRequest.of(page, size));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "단일 알림 읽음 처리")
  @PatchMapping("/{notificationId}/read")
  public ResponseEntity<ApiResponse<NotificationResDTO.Item>> markRead(
      @AuthenticationPrincipal String email, @PathVariable Long notificationId) {
    NotificationResDTO.Item result =
        notificationCommandService.markRead(resolveUserId(email), notificationId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "전체 알림 읽음 처리")
  @PatchMapping("/read-all")
  public ResponseEntity<ApiResponse<NotificationResDTO.ReadAll>> markAllRead(
      @AuthenticationPrincipal String email) {
    NotificationResDTO.ReadAll result =
        notificationCommandService.markAllRead(resolveUserId(email));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  private Long resolveUserId(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }
}
