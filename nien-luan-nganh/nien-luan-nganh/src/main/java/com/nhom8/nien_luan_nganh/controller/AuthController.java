package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.LoginRequest;
import com.nhom8.nien_luan_nganh.dto.request.RefreshTokenRequest;
import com.nhom8.nien_luan_nganh.dto.request.RegisterRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.AuthResponse;
import com.nhom8.nien_luan_nganh.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận và xử lý các yêu cầu HTTP liên quan đến Xác thực (Authentication).
 * Base URL: /api/v1/auth
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    // Khởi tạo tường minh
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * API 1: Đăng ký tài khoản mới.
     * Method: POST /api/v1/auth/register
     * Quyền: Công khai (PermitAll)
     *
     * @param request Dữ liệu đăng ký (username, identifier, password, confirmPassword,...)
     * @return HTTP 201 Created kèm AuthResponse (bộ token + thông tin user)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đăng ký tài khoản thành công"));
    }

    /**
     * API 2: Đăng nhập vào hệ thống.
     * Method: POST /api/v1/auth/login
     * Quyền: Công khai (PermitAll)
     *
     * @param request Dữ liệu đăng nhập (identifier Email/SĐT + password)
     * @return HTTP 200 OK kèm AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    /**
     * API 3: Làm mới Access Token khi token cũ hết hạn.
     * Method: POST /api/v1/auth/refresh
     * Quyền: Công khai (PermitAll)
     *
     * @param request Chứa refreshToken
     * @return HTTP 200 OK kèm AuthResponse chứa accessToken mới
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Làm mới token thành công"));
    }

    /**
     * API 4: Đăng xuất khỏi hệ thống.
     * Method: POST /api/v1/auth/logout
     * Quyền: Đã đăng nhập
     *
     * @param request Chứa refreshToken muốn thu hồi
     * @return HTTP 200 OK kèm thông báo thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request);
        }
        return ResponseEntity.ok(ApiResponse.message("Đăng xuất thành công"));
    }
}
