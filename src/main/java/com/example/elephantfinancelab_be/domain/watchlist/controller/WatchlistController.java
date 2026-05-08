package com.example.elephantfinancelab_be.domain.watchlist.controller;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.domain.watchlist.dto.req.WatchlistReqDTO;
import com.example.elephantfinancelab_be.domain.watchlist.dto.res.WatchlistResDTO;
import com.example.elephantfinancelab_be.domain.watchlist.service.command.WatchlistCommandService;
import com.example.elephantfinancelab_be.domain.watchlist.service.query.WatchlistQueryService;
import com.example.elephantfinancelab_be.global.apiPayload.ApiResponse;
import com.example.elephantfinancelab_be.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Watchlist", description = "관심목록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

  private final WatchlistQueryService watchlistQueryService;
  private final WatchlistCommandService watchlistCommandService;
  private final UserRepository userRepository;

  private Long resolveUserId(String email) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    return user.getId();
  }

  @Operation(summary = "관심그룹 목록 조회", description = "로그인한 사용자의 관심그룹 목록을 조회합니다.")
  @GetMapping("/groups")
  public ResponseEntity<ApiResponse<WatchlistResDTO.GroupListDTO>> getGroupList(
      @AuthenticationPrincipal String email) {
    WatchlistResDTO.GroupListDTO result = watchlistQueryService.findGroupList(resolveUserId(email));
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "새 관심그룹 생성", description = "새로운 관심그룹을 생성합니다.")
  @PostMapping("/groups")
  public ResponseEntity<ApiResponse<Void>> createGroup(
      @AuthenticationPrincipal String email,
      @Valid @RequestBody WatchlistReqDTO.CreateGroup request) {
    watchlistCommandService.saveGroup(resolveUserId(email), request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심그룹 수정", description = "관심그룹 이름을 수정합니다.")
  @PatchMapping("/groups/{groupId}")
  public ResponseEntity<ApiResponse<Void>> updateGroup(
      @AuthenticationPrincipal String email,
      @PathVariable Long groupId,
      @Valid @RequestBody WatchlistReqDTO.UpdateGroup request) {
    watchlistCommandService.updateGroup(resolveUserId(email), groupId, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심그룹 제거", description = "관심그룹을 삭제합니다.")
  @DeleteMapping("/groups/{groupId}")
  public ResponseEntity<ApiResponse<Void>> deleteGroup(
      @AuthenticationPrincipal String email, @PathVariable Long groupId) {
    watchlistCommandService.deleteGroup(resolveUserId(email), groupId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심종목 추가", description = "관심그룹에 종목을 추가합니다.")
  @PostMapping("/items")
  public ResponseEntity<ApiResponse<Void>> createItem(
      @AuthenticationPrincipal String email, @Valid @RequestBody WatchlistReqDTO.AddItem request) {
    watchlistCommandService.saveItem(resolveUserId(email), request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심종목 제거", description = "관심그룹에서 종목을 제거합니다.")
  @DeleteMapping("/items")
  public ResponseEntity<ApiResponse<Void>> deleteItem(
      @AuthenticationPrincipal String email,
      @Valid @RequestBody WatchlistReqDTO.RemoveItem request) {
    watchlistCommandService.deleteItem(resolveUserId(email), request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }
}
