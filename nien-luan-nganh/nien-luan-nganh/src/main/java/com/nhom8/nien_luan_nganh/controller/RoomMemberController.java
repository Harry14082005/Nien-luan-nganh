package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.*;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageReceiptResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.service.RoomMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/members")
@RequiredArgsConstructor
@Slf4j
public class RoomMemberController {
    private final RoomMemberService roomMemberService;
    private final SimpMessagingTemplate messagingTemplate;

    /*
    GET /api/v1/rooms/{roomId}/members                --> tất cả
    GET /api/v1/rooms/{roomId}/members?status=ACTIVE  --> đang hoạt động
    GET /api/v1/rooms/{roomId}/members?status=PENDING --> chờ duyệt
    */
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<RoomMemberResponse>>> getMembers(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestParam(required = false) RoomMemberStatus status
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<RoomMemberResponse> responses = roomMemberService.getMembers(roomId, status, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách thành viên thành công"));
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<List<RoomMemberResponse>>> addMembers(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestBody AddMemberRequest request
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<RoomMemberResponse> responses = roomMemberService.addMembers(roomId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Thêm thành viên thành công"));
    }

    // xoá thành viên
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<RoomMemberResponse>>> deleteMember(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestBody RemoveMemberRequest request
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        roomMemberService.removeMember(roomId, request.getTargetUserId(), currentUserId);

        return ResponseEntity.ok(ApiResponse.message("Xoá thành viên thành công"));
    }

    // rời phòng
    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<List<RoomMemberResponse>>> leaveRoom(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestBody LeaveRoomRequest request
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        roomMemberService.leaveRoom(roomId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.message("Rời phòng thành công"));
    }

    // sửa quyền thành viên
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<RoomMemberResponse>> changeMemberRole(
    //    public ResponseEntity<ApiResponse<RoomMemberResponse>> changeMemberRole(

            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID userId,
            @RequestBody ChangeMemberRoleRequest request
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomMemberResponse response = roomMemberService.changeMemberRole(roomId, userId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "Sửa quyền thành công"));
    }

    // phê duyệt thành viên
    @PutMapping("/{userId}/approve")
    public ResponseEntity<ApiResponse<RoomMemberResponse>> approveMember(
    //    public ResponseEntity<ApiResponse<RoomMemberResponse>> approveMember(

            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestBody ApproveMemberRequest request
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomMemberResponse response = roomMemberService.approveMember(roomId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.message("Phê duyệt thành viên thành công"));
    }

    // đánh dấu tin nhắn đã đọc
    @PutMapping("/me/read")
    public ResponseEntity<ApiResponse<MessageReceiptResponse>> markAsRead(
            @PathVariable UUID roomId,
            @RequestBody(required = false) MarkReadRequest request,
            Authentication authentication
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());
        UUID messageId = request != null ? request.getMessageId() : null;

        MessageReceiptResponse response = roomMemberService.markAsRead(roomId, messageId, currentUserId);

        if (response != null) {
            try {
                messagingTemplate.convertAndSend("/topic/room." + roomId + ".receipts", response);
            } catch (Exception ex) {
                log.warn("Không thể broadcast read receipt qua WebSocket cho room {}: {}", roomId, ex.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Đánh dấu đã đọc thành công"));
    }

    // đánh dấu tin nhắn đã nhận
    @PutMapping("/me/delivered")
    public ResponseEntity<ApiResponse<MessageReceiptResponse>> markAsDelivered(
            @PathVariable UUID roomId,
            @RequestBody(required = false) MarkReadRequest request,
            Authentication authentication
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());
        UUID messageId = request != null ? request.getMessageId() : null;

        MessageReceiptResponse response = roomMemberService.markAsDelivered(roomId, messageId, currentUserId);

        if (response != null) {
            try {
                messagingTemplate.convertAndSend("/topic/room." + roomId + ".receipts", response);
            } catch (Exception ex) {
                log.warn("Không thể broadcast delivered receipt qua WebSocket cho room {}: {}", roomId, ex.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Đánh dấu đã nhận thành công"));
    }

    // lấy danh sách thành viên có thể thêm vô phòng được
    @GetMapping("/invitable")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getInvitableMembers(
            @PathVariable UUID roomId,
            Authentication authentication
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<UserResponse> responses = roomMemberService.getInvitableMembers(roomId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách bạn bè có thể thêm thành công"));
    }
}
