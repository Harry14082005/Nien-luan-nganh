package com.nhom8.nien_luan_nganh.config;

import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

// Cầu nối giữa Spring Security vơi DB

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Nạp thông tin người dùng dựa trên định danh (identifier).
    // Hàm này được Spring Security tự động gọi khi:
    // 1. Xác thực đăng nhập qua AuthenticationManager (identifier lúc này là Email hoặc Số điện thoại).
    // 2. Giải mã JWT token trong Filter (identifier lúc này là UUID chuỗi được lưu trong subject).

    // @param identifier Chuỗi định danh: có thể là UUID (từ token) HOẶC Email/SĐT (từ form đăng nhập)
    // @return UserDetails Đối tượng UserDetailsImpl chứa thông tin người dùng
    // @throws UsernameNotFoundException Nếu không tìm thấy người dùng trong database
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Trường hợp 1: Thử parse định danh dưới dạng UUID 
        try {
            UUID userId = UUID.fromString(identifier);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                return new UserDetailsImpl(userOpt.get());
            }
        } catch (IllegalArgumentException ignored) {
            // Không phải chuỗi UUID hợp lệ, bỏ qua để tiếp tục tìm kiếm theo Email hoặc Số điện thoại
        }

        // Trường hợp 2: Tìm kiếm người dùng theo Email HOẶC Số điện thoại
        User user = userRepository.findByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với định danh: " + identifier));

        return new UserDetailsImpl(user);
    }

    // Phương thức tiện ích nạp trực tiếp UserDetails bằng UUID.
    // Thường dùng khi các Service hoặc Filter đã có sẵn đối tượng UUID.

    //@param userId Khóa chính UUID của người dùng
    //@return UserDetailsImpl chứa thông tin người dùng tương ứng
    //@throws UsernameNotFoundException Nếu ID không tồn tại trong database

    public UserDetails loadUserById(UUID userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        return new UserDetailsImpl(user);
    }
}
