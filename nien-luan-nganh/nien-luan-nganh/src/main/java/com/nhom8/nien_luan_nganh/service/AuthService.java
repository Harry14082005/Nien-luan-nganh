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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service xử lý các nghiệp vụ xác thực người dùng (Authentication):
 * - Đăng ký tài khoản mới (tự động phân biệt Email hoặc Số điện thoại).
 * - Đăng nhập hệ thống (kiểm tra tài khoản, mật khẩu băm BCrypt, trạng thái khóa tài khoản).
 * - Cấp mới Access Token qua Refresh Token.
 * - Đăng xuất.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    // Khởi tạo tường minh (Explicit Constructor) tránh lỗi Flexible Constructor Bodies trên VS Code
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    /**
     * Nghiệp vụ 1: Đăng ký tài khoản người dùng mới.
     * Quy trình:
     * 1. Kiểm tra xác nhận mật khẩu (confirmPassword) có khớp với password không.
     * 2. Phân biệt định danh (identifier) là Email hay Số điện thoại qua Regex.
     * 3. Kiểm tra xem Email hoặc Số điện thoại đó đã được sử dụng chưa.
     * 4. Băm mật khẩu bằng BCryptPasswordEncoder.
     * 5. Lưu User mới vào CSDL với vai trò USER mặc định.
     * 6. Sinh bộ Token (Access Token & Refresh Token) và trả về cho client.
     *
     * @param request Dữ liệu đăng ký từ client
     * @return AuthResponse Chứa token và thông tin profile cơ bản
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        // 2. Phân tích định danh (identifier): Email hay Số điện thoại
        String identifier = request.getIdentifier().trim();
        boolean isEmail = identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$");
        boolean isPhone = identifier.matches("^(0|\\+84)\\d{9}$");

        if (!isEmail && !isPhone) {
            throw new AppException(ErrorCode.IDENTIFIER_INVALID);
        }

        String email = null;
        String phoneNumber = null;

        if (isEmail) {
            // Kiểm tra trùng Email
            if (userRepository.existsByEmail(identifier)) {
                throw new AppException(ErrorCode.EMAIL_EXISTED);
            }
            email = identifier;
        } else {
            // Kiểm tra trùng Số điện thoại
            if (userRepository.existsByPhoneNumber(identifier)) {
                throw new AppException(ErrorCode.PHONE_NUMBER_EXISTED);
            }
            phoneNumber = identifier;
        }

        // 3. Mã hóa mật khẩu người dùng bằng BCrypt
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 4. Khởi tạo thực thể User
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(encodedPassword)
                .email(email)
                .phoneNumber(phoneNumber)
                .gender(request.getGender())
                .preferredLanguage(request.getPreferredLanguage() != null && !request.getPreferredLanguage().isBlank()
                        ? request.getPreferredLanguage().trim()
                        : "vi")
                .role(UserRole.USER)
                .isBanned(false)
                .build();

        // 5. Lưu vào Database MySQL
        user = userRepository.save(user);

        // 6. Sinh Access Token và Refresh Token
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 7. Đóng gói kết quả trả về
        return AuthResponse.of(accessToken, refreshToken, userMapper.toUserResponse(user));
    }

    /**
     * Nghiệp vụ 2: Đăng nhập vào hệ thống.
     * Quy trình:
     * 1. Tìm người dùng theo Email HOẶC Số điện thoại qua query repository.
     * 2. Kiểm tra nếu tài khoản đang bị Admin khóa (isBanned = true) -> Chặn đăng nhập (HTTP 403).
     * 3. Kiểm tra tính trùng khớp của mật khẩu qua passwordEncoder.matches().
     * 4. Cập nhật thời điểm hoạt động cuối cùng (lastSeenAt = now).
     * 5. Sinh bộ Token mới và trả về cho client.
     *
     * @param request Thông tin đăng nhập gồm identifier và password
     * @return AuthResponse Token và profile người dùng
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        // 1. Tìm user theo Email HOẶC Số điện thoại
        User user = userRepository.findByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // 2. Kiểm tra trạng thái khóa tài khoản
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        // 3. Đối soát mật khẩu thô gửi lên với mật khẩu băm trong database
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 4. Cập nhật thời điểm hoạt động gần nhất (lastSeenAt)
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        // 5. Sinh Access Token và Refresh Token mới
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.of(accessToken, refreshToken, userMapper.toUserResponse(user));
    }

    /**
     * Nghiệp vụ 3: Làm mới Access Token (Token Refresh).
     * Quy trình:
     * 1. Kiểm tra Refresh Token còn hạn sử dụng và hợp lệ không.
     * 2. Trích xuất userId từ Subject của Refresh Token.
     * 3. Nạp thông tin user từ CSDL và kiểm tra tài khoản có bị khóa không.
     * 4. Sinh Access Token mới và trả về.
     *
     * @param request Chứa chuỗi refreshToken
     * @return AuthResponse Chứa Access Token mới
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Kiểm tra tính hợp lệ và hạn sử dụng của Refresh Token
        UUID userId;
        try {
            if (jwtService.isTokenExpired(refreshToken)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
            userId = jwtService.extractUserId(refreshToken);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 2. Tìm kiếm người dùng trong cơ sở dữ liệu
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 3. Nếu tài khoản bị khóa trong thời gian đang dùng thì không cấp token mới
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new AppException(ErrorCode.USER_BANNED);
        }

        // 4. Sinh Access Token mới (giữ nguyên Refresh Token hiện tại)
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.of(newAccessToken, refreshToken, userMapper.toUserResponse(user));
    }

    /**
     * Nghiệp vụ 4: Đăng xuất tài khoản.
     * Client sẽ tự động xóa accessToken và refreshToken khỏi LocalStorage/Cookie.
     * (Ở giai đoạn tích hợp Redis sau này, ta có thể lưu token vào Blacklist để vô hiệu hóa ngay lập tức).
     *
     * @param request Chứa thông tin refreshToken muốn thu hồi
     */
    public void logout(RefreshTokenRequest request) {
        // Thực hiện ghi log hoặc thu hồi token nếu cần thiết
    }
}
