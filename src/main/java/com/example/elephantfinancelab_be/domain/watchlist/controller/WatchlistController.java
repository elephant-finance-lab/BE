package com.example.elephantfinancelab_be.domain.watchlist.controller;

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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Watchlist", description = "관심목록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watchlist")
public class WatchlistController {

  private final WatchlistQueryService watchlistQueryService;
  private final WatchlistCommandService watchlistCommandService;

  @Operation(summary = "관심그룹 목록 조회", description = "로그인한 사용자의 관심그룹 목록을 조회합니다.")
  @GetMapping("/groups")
  public ResponseEntity<ApiResponse<WatchlistResDTO.GroupListDTO>> getGroupList() {
    // TODO: Security Context에서 userId 추출로 교체
    Long userId = 1L;
    WatchlistResDTO.GroupListDTO result = watchlistQueryService.findGroupList(userId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, result));
  }

  @Operation(summary = "새 관심그룹 생성", description = "새로운 관심그룹을 생성합니다.")
  @PostMapping("/groups")
  public ResponseEntity<ApiResponse<Void>> createGroup(
      @Valid @RequestBody WatchlistReqDTO.CreateGroup request) {
    Long userId = 1L;
    watchlistCommandService.saveGroup(userId, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심그룹 수정", description = "관심그룹 이름을 수정합니다.")
  @PatchMapping("/groups/{groupId}")
  public ResponseEntity<ApiResponse<Void>> updateGroup(
      @PathVariable Long groupId, @Valid @RequestBody WatchlistReqDTO.UpdateGroup request) {
    Long userId = 1L;
    watchlistCommandService.updateGroup(userId, groupId, request);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }

  @Operation(summary = "관심그룹 제거", description = "관심그룹을 삭제합니다.")
  @DeleteMapping("/groups/{groupId}")
  public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
    Long userId = 1L;
    watchlistCommandService.deleteGroup(userId, groupId);
    return ResponseEntity.status(GeneralSuccessCode.OK.getStatus())
        .body(ApiResponse.of(GeneralSuccessCode.OK, null));
  }
}
