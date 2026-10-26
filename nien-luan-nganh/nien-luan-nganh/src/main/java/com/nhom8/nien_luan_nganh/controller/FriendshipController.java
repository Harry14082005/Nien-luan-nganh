package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.BlockUserRequest;
import com.nhom8.nien_luan_nganh.dto.request.FriendshipRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.FriendshipResponse;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import com.nhom8.nien_luan_nganh.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến Phân hệ Kết nối Bạn bè (Friendship).
 * Base URL: /api/v1/friendships (hỗ trợ cả /api/v1/friends).
 * Tất cả các API đều yêu cầu người dùng phải đăng nhập (gửi kèm Bearer Token).
 */
@RestController
@RequestMapping({"/api/v1/friendships", "/api/v1/friends"})
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    /**
     * API 1: Gửi lời mời kết bạn.
     * Quyền: Đã đăng nhập
     *
     * @param authentication Thông tin người gửi từ Token
     * @param request        Chứa friendId của người nhận
     * @return HTTP 201 Created kèm thông tin FriendshipResponse
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            Authentication authentication,
            @Valid @RequestBody FriendshipRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        FriendshipResponse response = friendshipService.sendFriendRequest(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Gửi lời mời kết bạn thành công"));
    }

    /**
     * API 2: Chấp nhận lời mời kết bạn.
     * Quyền: Đã đăng nhập (phải là người nhận lời mời)
     *
     * @param authentication Thông tin người nhận từ Token
     * @param friendshipId   Khóa chính UUID của mối quan hệ
     * @return HTTP 200 OK kèm thông tin quan hệ sau khi chuyển sang ACCEPTED
     */
    @PutMapping("/{friendshipId}/accept")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptFriendRequest(
            Authentication authentication,
            @PathVariable UUID friendshipId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        FriendshipResponse response = friendshipService.acceptFriendRequest(currentUserId, friendshipId);
        return ResponseEntity.ok(ApiResponse.success(response, "Chấp nhận lời mời kết bạn thành công"));
    }

    /**
     * API 3: Từ chối lời mời kết bạn.
     * Quyền: Đã đăng nhập
     *
     * @param authentication Thông tin người nhận từ Token
     * @param friendshipId   Khóa chính UUID của mối quan hệ
     * @return HTTP 200 OK
     */
    @PutMapping("/{friendshipId}/decline")
    public ResponseEntity<ApiResponse<FriendshipResponse>> declineFriendRequest(
            Authentication authentication,
            @PathVariable UUID friendshipId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        FriendshipResponse response = friendshipService.declineFriendRequest(currentUserId, friendshipId);
        return ResponseEntity.ok(ApiResponse.success(response, "Từ chối lời mời kết bạn thành công"));
    }

    /**
     * API 4: Hủy kết bạn hoặc Thu hồi lời mời đã gửi.
     * Quyền: Đã đăng nhập
     *
     * @param authentication Thông tin người dùng từ Token
     * @param friendshipId   Khóa chính UUID của mối quan hệ
     * @return HTTP 200 OK
     */
    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrUnfriend(
            Authentication authentication,
            @PathVariable UUID friendshipId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        friendshipService.cancelOrUnfriend(currentUserId, friendshipId);
        return ResponseEntity.ok(ApiResponse.message("Xóa mối quan hệ bạn bè thành công"));
    }

    /**
     * API 5: Chặn người dùng.
     * Method: POST /api/v1/friendships/block
     * Quyền: Đã đăng nhập
     *
     * @param authentication Thông tin người chặn từ Token
     * @param request        Chứa userId của người bị chặn
     * @return HTTP 200 OK kèm FriendshipResponse có status = BLOCKED
     */
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<FriendshipResponse>> blockUser(
            Authentication authentication,
            @Valid @RequestBody BlockUserRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        FriendshipResponse response = friendshipService.blockUser(currentUserId, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Đã chặn người dùng thành công"));
    }

    /**
     * API 6: Bỏ chặn người dùng.
     * Quyền: Đã đăng nhập (chỉ người đã chặn mới được bỏ chặn)
     *
     * @param authentication Thông tin người bỏ chặn từ Token
     * @param userId         Khóa chính UUID của người đang bị chặn
     * @return HTTP 200 OK
     */
    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            Authentication authentication,
            @PathVariable UUID userId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        friendshipService.unblockUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.message("Đã bỏ chặn người dùng thành công"));
    }

    /**
     * API 7: Lấy danh sách mối quan hệ theo trạng thái.
     * - status=ACCEPTED (mặc định): Danh sách bạn bè hiện tại.
     * - status=PENDING: Danh sách lời mời kết bạn (có phân biệt direction: INCOMING hoặc OUTGOING).
     * - status=BLOCKED: Danh sách những người tôi đã chặn.
     *
     * @param authentication Thông tin người dùng từ Token
     * @param status         Trạng thái cần lọc (ACCEPTED, PENDING, BLOCKED)
     * @return HTTP 200 OK kèm danh sách FriendshipResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriendships(
            Authentication authentication,
            @RequestParam(name = "status", required = false, defaultValue = "ACCEPTED") FriendshipStatus status) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<FriendshipResponse> response = friendshipService.getFriendships(currentUserId, status);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách bạn bè thành công"));
    }

    /**
     * API 8: Kiểm tra trạng thái quan hệ giữa người đang đăng nhập và một người dùng khác.
     * Hỗ trợ Frontend kiểm tra và render đúng nút bấm trên trang cá nhân đối phương:
     * - null: Chưa có quan hệ -> hiện nút [Thêm bạn bè]
     * - PENDING + INCOMING: Có người gửi đến -> chỉ hiện 2 nút [Xác nhận] và [Xóa yêu cầu]
     * - PENDING + OUTGOING: Đã gửi đi -> hiện nút [Đã gửi lời mời / Hủy]
     * - ACCEPTED: Đã là bạn bè -> hiện nút [Bạn bè / Nhắn tin]
     * - BLOCKED: Đã chặn
     *
     * @param authentication Thông tin người dùng từ Token
     * @param userId         Khóa chính UUID của người dùng cần kiểm tra
     * @return HTTP 200 OK kèm FriendshipResponse (hoặc data null nếu chưa có quan hệ)
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> getFriendshipStatus(
            Authentication authentication,
            @PathVariable UUID userId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        FriendshipResponse response = friendshipService.getFriendshipStatusWithUser(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy trạng thái quan hệ thành công"));
    }
}
