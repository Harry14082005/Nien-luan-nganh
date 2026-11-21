package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateRoomNameRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomInviteInfoResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // tạo nhóm nhắn tin
    @PostMapping("/group")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateGroupRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomDetailResponse response = roomService.createGroup(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo nhóm thành công"));

    }

    // kết nối trực tiếp //có thể cần xử lý thêm trường hợp lần sau ấn (tại vì nó
    // không tạo mới)
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> createDirectRoom(
            Authentication authentication,
            @Valid @RequestBody CreateDirectRoomRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomDetailResponse response = roomService.createDirectRoom(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo phòng trực tiếp thành công"));
    }

    // lấy danh sách phòng
    @GetMapping("")
    public ResponseEntity<ApiResponse<PageResponse<RoomListResponse>>> getRooms(
            Authentication authentication,
            @RequestParam(required = false) RoomType type,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        PageResponse<RoomListResponse> responses = roomService.getRooms(currentUserId, type, pageNo, pageSize);

        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách phòng thành công"));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoom(
            Authentication authentication,
            @PathVariable UUID roomId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomDetailResponse response = roomService.getRoom(roomId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin phòng thành công"));
    }

    /**
     * API Cập nhật avatar phòng / nhóm chat
     * POST /api/v1/rooms/{roomId}/avatar
     */
    @PostMapping(value = "/{roomId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomDetailResponse>> updateRoomAvatar(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        RoomDetailResponse response = roomService.updateRoomAvatar(roomId, currentUserId, file);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật avatar nhóm thành công"));
    }

    /**
     * API Cập nhật tên phòng / nhóm chat
     * PUT /api/v1/rooms/{roomId}/name
     */
    @PutMapping("/{roomId}/name")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> updateRoomName(
            Authentication authentication,
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomNameRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        RoomDetailResponse response = roomService.updateRoomName(roomId, currentUserId, request.getRoomName());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật tên nhóm thành công"));
    }

    /**
     * API 1: Lấy link mời của nhóm (chỉ dành cho Admin / Trưởng nhóm)
     * GET /api/v1/rooms/{roomId}/invite-link
     */
    @GetMapping("/{roomId}/invite-link")
    public ResponseEntity<ApiResponse<String>> getRoomInviteLink(
            Authentication authentication,
            @PathVariable UUID roomId) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        String inviteCode = roomService.getRoomInviteLink(roomId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(inviteCode, "Lấy liên kết mời thành công"));
    }

    /**
     * API 2: Lấy thông tin tóm tắt của nhóm theo inviteCode (hiển thị trang
     * /join/:inviteCode)
     * GET /api/v1/rooms/invite/{inviteCode}
     */
    @GetMapping("/invite/{inviteCode}")
    public ResponseEntity<ApiResponse<RoomInviteInfoResponse>> getRoomInviteInfo(
            Authentication authentication,
            @PathVariable String inviteCode) {
        UUID currentUserId = (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().equals("anonymousUser"))
                        ? UUID.fromString(authentication.getName())
                        : null;
        RoomInviteInfoResponse response = roomService.getRoomInviteInfo(inviteCode, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin nhóm thành công"));
    }

    /**
     * API 3: Tham gia nhóm qua inviteCode
     * POST /api/v1/rooms/invite/{inviteCode}/join
     */
    @PostMapping("/invite/{inviteCode}/join")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> joinRoomByInviteCode(
            Authentication authentication,
            @PathVariable String inviteCode) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        RoomDetailResponse response = roomService.joinRoomByInviteCode(inviteCode, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tham gia nhóm thành công"));
    }
}
