package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.UpdateProfileRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến Hồ sơ Người dùng (Users).
 * Base URL: /api/v1/users
 * Tất cả các API trong Controller này đều yêu cầu người dùng phải đăng nhập (gửi kèm Bearer Token).
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * API: Lấy thông tin hồ sơ của chính người dùng đang đăng nhập.
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
     * API: Cập nhật hồ sơ cá nhân (tên, avatar, giới tính, tiểu sử bio, ngôn ngữ).
     * Method: PUT /api/v1/users/me
     * Quyền: Đã đăng nhập (Gửi kèm Bearer Token)
     *
     * @param authentication Thông tin người dùng từ Token
     * @param request Dữ liệu cập nhật từ client
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
}
