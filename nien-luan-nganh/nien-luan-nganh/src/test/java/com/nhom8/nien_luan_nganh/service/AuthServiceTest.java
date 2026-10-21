package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.config.JwtService;
import com.nhom8.nien_luan_nganh.dto.request.LoginRequest;
import com.nhom8.nien_luan_nganh.dto.request.RefreshTokenRequest;
import com.nhom8.nien_luan_nganh.dto.request.RegisterRequest;
import com.nhom8.nien_luan_nganh.dto.response.AuthResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.UserRole;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị (Unit Test) cho AuthService.
 * Sử dụng Mockito để cô lập tầng nghiệp vụ, không cần khởi chạy Database hay Spring Context thực tế.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UserResponse sampleUserResponse;
    private RegisterRequest registerRequestEmail;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(UUID.randomUUID())
                .username("nguyenvana")
                .email("test@gmail.com")
                .phoneNumber("0987654321")
                .password("$2a$10$hashedPasswordSample")
                .role(UserRole.USER)
                .isBanned(false)
                .preferredLanguage("vi")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleUserResponse = UserResponse.builder()
                .userId(sampleUser.getUserId())
                .username(sampleUser.getUsername())
                .email(sampleUser.getEmail())
                .phoneNumber(sampleUser.getPhoneNumber())
                .role(sampleUser.getRole())
                .isBanned(false)
                .build();

        registerRequestEmail = RegisterRequest.builder()
                .username("nguyenvana")
                .identifier("test@gmail.com")
                .password("Password123")
                .confirmPassword("Password123")
                .gender("MALE")
                .preferredLanguage("vi")
                .build();

        loginRequest = LoginRequest.builder()
                .identifier("test@gmail.com")
                .password("Password123")
                .build();
    }

    // ==========================================
    // 1. KIỂM THỬ ĐĂNG KÝ (REGISTER)
    // ==========================================

    @Test
    @DisplayName("Đăng ký thành công với Email")
    void register_success_withEmail() {
        // Giả lập
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("mockAccessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("mockRefreshToken");
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        // Thực thi
        AuthResponse response = authService.register(registerRequestEmail);

        // Kiểm tra
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals("test@gmail.com", response.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thành công với Số điện thoại")
    void register_success_withPhone() {
        RegisterRequest phoneRequest = RegisterRequest.builder()
                .username("nguyenvanb")
                .identifier("0912345678")
                .password("Secret123")
                .confirmPassword("Secret123")
                .gender("FEMALE")
                .preferredLanguage("en")
                .build();

        User phoneUser = User.builder()
                .userId(UUID.randomUUID())
                .username("nguyenvanb")
                .phoneNumber("0912345678")
                .role(UserRole.USER)
                .isBanned(false)
                .build();

        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encodedPhonePass");
        when(userRepository.save(any(User.class))).thenReturn(phoneUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessPhone");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshPhone");

        UserResponse phoneResponse = UserResponse.builder()
                .userId(phoneUser.getUserId())
                .username("nguyenvanb")
                .phoneNumber("0912345678")
                .role(UserRole.USER)
                .build();
        when(userMapper.toUserResponse(any(User.class))).thenReturn(phoneResponse);

        AuthResponse response = authService.register(phoneRequest);

        assertNotNull(response);
        assertEquals("0912345678", response.getUser().getPhoneNumber());
        assertNull(response.getUser().getEmail());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi mật khẩu xác nhận không khớp")
    void register_fail_passwordNotMatch() {
        registerRequestEmail.setConfirmPassword("WrongConfirmPassword");

        AppException ex = assertThrows(AppException.class, () -> authService.register(registerRequestEmail));
        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Đăng ký thất bại khi định danh không phải Email cũng không phải Số điện thoại")
    void register_fail_invalidIdentifier() {
        registerRequestEmail.setIdentifier("invalid_identifier_format");

        AppException ex = assertThrows(AppException.class, () -> authService.register(registerRequestEmail));
        assertEquals(ErrorCode.IDENTIFIER_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("Đăng ký thất bại khi Email đã tồn tại trong CSDL")
    void register_fail_emailAlreadyExists() {
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(registerRequestEmail));
        assertEquals(ErrorCode.EMAIL_EXISTED, ex.getErrorCode());
    }

    // ==========================================
    // 2. KIỂM THỬ ĐĂNG NHẬP (LOGIN)
    // ==========================================

    @Test
    @DisplayName("Đăng nhập thành công với tài khoản và mật khẩu đúng")
    void login_success() {
        when(userRepository.findByEmailOrPhoneNumber("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("accessLogged");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refreshLogged");
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("accessLogged", response.getAccessToken());
        assertEquals("refreshLogged", response.getRefreshToken());
        verify(userRepository, times(1)).save(sampleUser); // Cập nhật lastSeenAt
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản không tồn tại")
    void login_fail_userNotFound() {
        when(userRepository.findByEmailOrPhoneNumber("test@gmail.com")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.login(loginRequest));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa bởi Quản trị viên (isBanned = true)")
    void login_fail_userBanned() {
        sampleUser.setIsBanned(true);
        when(userRepository.findByEmailOrPhoneNumber("test@gmail.com")).thenReturn(Optional.of(sampleUser));

        AppException ex = assertThrows(AppException.class, () -> authService.login(loginRequest));
        assertEquals(ErrorCode.USER_BANNED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void login_fail_wrongPassword() {
        when(userRepository.findByEmailOrPhoneNumber("test@gmail.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123", sampleUser.getPassword())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(loginRequest));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    // ==========================================
    // 3. KIỂM THỬ LÀM MỚI TOKEN (REFRESH TOKEN)
    // ==========================================

    @Test
    @DisplayName("Làm mới token thành công khi Refresh Token hợp lệ")
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
        UUID userId = sampleUser.getUserId();

        when(jwtService.isTokenExpired("validRefreshToken")).thenReturn(false);
        when(jwtService.extractUserId("validRefreshToken")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("newAccessToken");
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("validRefreshToken", response.getRefreshToken());
    }

    @Test
    @DisplayName("Làm mới token thất bại khi Refresh Token đã hết hạn")
    void refreshToken_fail_expired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expiredToken");
        when(jwtService.isTokenExpired("expiredToken")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }
}
