package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.AddPollOptionRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreatePollRequest;
import com.nhom8.nien_luan_nganh.dto.request.VotePollRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.PollResponse;
import com.nhom8.nien_luan_nganh.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/room/{roomId}/polls", "/api/v1/room/{roomId}/poll"})
@RequiredArgsConstructor
public class PollController {
    private final PollService pollService;

    @PostMapping
    public ResponseEntity<ApiResponse<PollResponse>> createPoll(
            Authentication auth, @PathVariable UUID roomId,
            @Valid @RequestBody CreatePollRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.createPoll(roomId, request, currentUserId), "Tạo bình chọn thành công"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PollResponse>>> getPolls(Authentication auth, @PathVariable UUID roomId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.getPolls(roomId, currentUserId), "Lấy danh sách bình chọn thành công"));
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollResponse>> getPoll(Authentication auth, @PathVariable UUID roomId, @PathVariable UUID pollId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.getPoll(roomId, pollId, currentUserId), "Lấy bình chọn thành công"));
    }

    @PostMapping("/{pollId}/options")
    public ResponseEntity<ApiResponse<PollResponse>> addOption(
            Authentication auth, @PathVariable UUID roomId, @PathVariable UUID pollId,
            @Valid @RequestBody AddPollOptionRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.addOption(roomId, pollId, request, currentUserId), "Thêm lựa chọn thành công"));
    }

    @PutMapping("/{pollId}/vote")
    public ResponseEntity<ApiResponse<PollResponse>> vote(
            Authentication auth,
            @PathVariable UUID roomId,
            @PathVariable UUID pollId,
            @Valid @RequestBody VotePollRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.vote(roomId, pollId, request, currentUserId), "Bình chọn thành công"));
    }

    @PutMapping("/{pollId}/close")
    public ResponseEntity<ApiResponse<PollResponse>> closePoll(Authentication auth, @PathVariable UUID roomId, @PathVariable UUID pollId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(pollService.closePoll(roomId, pollId, currentUserId), "Đã kết thúc bình chọn"));
    }
}
