package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.SendMessageRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateMessageRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.CursorResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageHistoryResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.nhom8.nien_luan_nganh.dto.request.ReactionRequest;
import com.nhom8.nien_luan_nganh.dto.response.ReactionResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến Tin nhắn trong
 * phòng chat.
 * Base URL: /api/v1/rooms/{roomId}/messages
 * Yêu cầu: Người dùng phải đăng nhập (gửi kèm JWT Bearer Token).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    /**
     * API: Lấy danh sách tin nhắn trong phòng chat theo phân trang con trỏ
     * (Cursor-based Pagination).
     * Method: GET /api/v1/rooms/{roomId}/messages
     * Hỗ trợ alias tham số: before hoặc cursor.
     * Quyền: Đã đăng nhập và phải là thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param cursor         Khóa chính UUID tin nhắn mốc phân trang (tùy chọn)
     * @param before         Alias cho cursor (tùy chọn)
     * @param limit          Số lượng tin nhắn cần lấy (mặc định 20)
     * @return HTTP 200 OK kèm dữ liệu CursorResponse<MessageResponse>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CursorResponse<MessageResponse>>> getMessages(
            Authentication authentication,
            @PathVariable UUID roomId,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "20") int limit) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        UUID cursorId = (cursor != null) ? cursor : before;

        CursorResponse<MessageResponse> response = messageService.getMessages(currentUserId, roomId, cursorId, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách tin nhắn thành công"));
    }

    /**
     * API 1: Gửi tin nhắn vào phòng chat (Hỗ trợ Text, Image, File đính kèm và
     * Reply tin nhắn).
     * Method: POST /api/v1/rooms/{roomId}/messages
     * Quyền: Đã đăng nhập và phải là thành viên trong phòng chat.
     *
     * @param authentication Thông tin người gửi từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param request        Nội dung, loại tin nhắn và ID tin nhắn cha (nếu có
     *                       reply)
     * @return HTTP 201 Created kèm dữ liệu MessageResponse
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @Valid @RequestBody SendMessageRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        MessageResponse response = messageService.sendMessage(currentUserId, roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Gửi tin nhắn thành công"));
    }

    /**
     * API 2: Chỉnh sửa nội dung tin nhắn.
     * Method: PUT /api/v1/rooms/{roomId}/messages/{messageId}
     * Quyền: Đã đăng nhập và chỉ người gửi tin nhắn mới được sửa tin nhắn của mình.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn cần sửa
     * @param request        Nội dung chỉnh sửa mới
     * @return HTTP 200 OK kèm dữ liệu MessageResponse sau khi cập nhật
     */
    @PutMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> updateMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateMessageRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        MessageResponse response = messageService.updateMessage(currentUserId, roomId, messageId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Chỉnh sửa tin nhắn thành công"));
    }

    /**
     * API 3: Thu hồi tin nhắn (Xóa mềm).
     * Method: DELETE /api/v1/rooms/{roomId}/messages/{messageId}
     * Quyền: Người gửi tin nhắn hoặc Trưởng nhóm/Quản trị viên phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn cần thu hồi
     * @return HTTP 200 OK kèm dữ liệu MessageResponse với nội dung đã được thu hồi
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageResponse>> revokeMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        MessageResponse response = messageService.revokeMessage(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Thu hồi tin nhắn thành công"));
    }

    /**
     * API 4: Xem lịch sử các lần chỉnh sửa của tin nhắn.
     * Method: GET /api/v1/rooms/{roomId}/messages/{messageId}/history
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn
     * @return HTTP 200 OK kèm danh sách MessageHistoryResponse
     */
    @GetMapping("/{messageId}/history")
    public ResponseEntity<ApiResponse<List<MessageHistoryResponse>>> getMessageHistory(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<MessageHistoryResponse> response = messageService.getMessageHistory(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy lịch sử chỉnh sửa tin nhắn thành công"));
    }

    /**
     * API 5: Ghim tin nhắn trong phòng chat.
     * Method: POST /api/v1/rooms/{roomId}/messages/{messageId}/pins
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn cần ghim
     * @return HTTP 200 OK kèm dữ liệu MessageResponse sau khi ghim
     */
    @PostMapping("/{messageId}/pins")
    public ResponseEntity<ApiResponse<MessageResponse>> pinMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        MessageResponse response = messageService.pinMessage(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ghim tin nhắn thành công"));
    }

    /**
     * API 6: Bỏ ghim tin nhắn trong phòng chat.
     * Method: DELETE /api/v1/rooms/{roomId}/messages/{messageId}/pins
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn cần bỏ ghim
     * @return HTTP 200 OK kèm dữ liệu MessageResponse sau khi bỏ ghim
     */
    @DeleteMapping("/{messageId}/pins")
    public ResponseEntity<ApiResponse<MessageResponse>> unpinMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        MessageResponse response = messageService.unpinMessage(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Bỏ ghim tin nhắn thành công"));
    }

    /**
     * API 7: Lấy danh sách tin nhắn đã ghim trong phòng chat.
     * Method: GET /api/v1/rooms/{roomId}/messages/pins
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @return HTTP 200 OK kèm danh sách MessageResponse của các tin nhắn đang ghim
     */
    @GetMapping("/pins")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getPinnedMessages(
            Authentication authentication,
            @PathVariable UUID roomId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<MessageResponse> response = messageService.getPinnedMessages(currentUserId, roomId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách tin nhắn đã ghim thành công"));
    }

    /**
     * API 8: Thả hoặc cập nhật cảm xúc trên tin nhắn.
     * Method: POST /api/v1/rooms/{roomId}/messages/{messageId}/reactions
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn
     * @param request        Loại cảm xúc (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)
     * @return HTTP 200 OK kèm dữ liệu ReactionResponse
     */
    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<ReactionResponse>> reactMessage(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody ReactionRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        ReactionResponse response = messageService.reactMessage(currentUserId,
                roomId, messageId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thả cảm xúc thành công"));
    }

    /**
     * API 9: Gỡ cảm xúc đã thả trên tin nhắn.
     * Method: DELETE /api/v1/rooms/{roomId}/messages/{messageId}/reactions
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn
     * @return HTTP 200 OK với thông báo gỡ thành công
     */
    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        messageService.removeReaction(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Gỡ cảm xúc thành công"));
    }

    /**
     * API 10: Xem danh sách các cảm xúc đã thả trên tin nhắn.
     * Method: GET /api/v1/rooms/{roomId}/messages/{messageId}/reactions
     * Quyền: Thành viên trong phòng chat.
     *
     * @param authentication Thông tin người dùng từ JWT Token
     * @param roomId         Khóa chính UUID của phòng chat
     * @param messageId      Khóa chính UUID của tin nhắn
     * @return HTTP 200 OK kèm danh sách ReactionResponse
     */
    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<List<ReactionResponse>>> getMessageReactions(
            Authentication authentication,
            @PathVariable UUID roomId,
            @PathVariable UUID messageId) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        List<ReactionResponse> response = messageService.getMessageReactions(currentUserId, roomId, messageId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách cảm xúc thành công"));
    }
}
