package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.ChangePasswordRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateEmailRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdatePhoneRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateProfileRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến Hồ sơ Người dùng
 * (Users).
 * Base URL hỗ trợ cả /api/v1/users và /api/v1/user để tương thích chuẩn API.
 * Tất cả các API trong Controller này đều yêu cầu người dùng phải đăng nhập
 * (gửi kèm Bearer Token).
 */
@RestController
@RequestMapping({ "/api/v1/users", "/api/v1/user" })
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * API 1: Lấy thông tin hồ sơ của chính người dùng đang đăng nhập.
     * Method: GET /api/v1/users/me
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng đã được xác thực từ Token
     * @return HTTP 200 OK kèm thông tin UserResponse
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        // authentication.getName() trả về chuỗi UUID đã được nạp trong UserDetailsImpl
        UUID currentUserId = UUID.fromString(authentication.getName());

        UserResponse response = userService.getMyProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin cá nhân thành công"));
    }

    /**
     * API 2: Cập nhật hồ sơ cá nhân (tên, avatar, giới tính, tiểu sử bio, ngôn
     * ngữ).
     * Method: PUT /api/v1/users/me
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng từ Token
     * @param request        Dữ liệu cập nhật từ client
     * @return HTTP 200 OK kèm thông tin UserResponse sau khi cập nhật
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        UserResponse response = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ thành công"));
    }

    /**
     * API 3: Đổi mật khẩu tài khoản cá nhân.
     * Method: PUT /api/v1/users/me/password (hoặc /api/v1/user/me/password)
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng từ Token
     * @param request Chứa oldPassword, newPassword, confirmNewPassword
     * @return HTTP 200 OK
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        userService.changePassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.message("Đổi mật khẩu thành công"));
    }


    /**
     * API 4: Đổi địa chỉ Email tài khoản cá nhân.
     * Method: PUT /api/v1/users/me/email (hoặc /api/v1/user/me/email)
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng từ Token
     * @param request        Chứa email mới
     * @return HTTP 200 OK kèm thông tin hồ sơ mới
     */
    @PutMapping("/me/email")
    public ResponseEntity<ApiResponse<UserResponse>> updateEmail(
            Authentication authentication,
            @Valid @RequestBody UpdateEmailRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        UserResponse response = userService.updateEmail(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật email thành công"));
    }

    /**
     * API 5: Đổi Số điện thoại tài khoản cá nhân.
     * Method: PUT /api/v1/users/me/phone (hoặc /api/v1/user/me/phone)
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng từ Token
     * @param request        Chứa phoneNumber mới
     * @return HTTP 200 OK kèm thông tin hồ sơ mới
     */
    @PutMapping("/me/phone")
    public ResponseEntity<ApiResponse<UserResponse>> updatePhone(
            Authentication authentication,
            @Valid @RequestBody UpdatePhoneRequest request) {
        UUID currentUserId = UUID.fromString(authentication.getName());

        UserResponse response = userService.updatePhone(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật số điện thoại thành công"));
    }

    /**
     * API 6: Tìm kiếm người dùng theo Email, Số điện thoại hoặc Tên hiển thị.
     * Method: GET /api/v1/users/search?q=...&page=0&size=10
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng hiện tại (dùng để loại trừ chính họ khỏi kết quả)
     * @param query          Từ khóa tìm kiếm truyền qua Query Param (?q=...)
     * @param page           Số thứ tự trang, bắt đầu từ 0 (mặc định 0)
     * @param size           Số lượng kết quả mỗi trang (mặc định 10)
     * @return HTTP 200 OK kèm dữ liệu phân trang PageResponse<UserResponse>
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            Authentication authentication,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<UserResponse> response = userService.searchUsers(query, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Tìm kiếm người dùng thành công"));
    }

    /**
     * API 7: Xem trang cá nhân công khai của người dùng khác.
     * Method: GET /api/v1/users/{userId}
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param userId Khóa chính UUID của người dùng cần xem
     * @return HTTP 200 OK kèm thông tin UserResponse
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin người dùng thành công"));
    }
}
