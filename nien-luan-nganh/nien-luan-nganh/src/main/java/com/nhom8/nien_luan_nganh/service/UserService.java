package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.UpdateProfileRequest;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến Hồ sơ Người dùng (Users Module):
 * - Lấy thông tin cá nhân của người đang đăng nhập (/users/me).
 * - Cập nhật thông tin cá nhân (bio, avatar, tên, giới tính, ngôn ngữ).
 * - Đổi mật khẩu, đổi email, đổi số điện thoại (sẽ bổ sung ở các bước sau).
 * - Tìm kiếm người dùng và xem trang cá nhân người khác.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    /**
     * Nghiệp vụ 1: Lấy thông tin hồ sơ của chính người dùng đang đăng nhập.
     *
     * @param currentUserId Khóa chính UUID của người dùng trích xuất từ Token
     * @return UserResponse Thông tin người dùng an toàn (không có password)
     */


    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UUID currentUserId){
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    /**
     * Nghiệp vụ 2: Cập nhật thông tin hồ sơ cá nhân.
     * Chỉ cập nhật những trường mà người dùng có truyền dữ liệu lên (không bị null).
     *
     * @param currentUserId Khóa chính UUID của người dùng
     * @param request Dữ liệu cập nhật từ client
     * @return UserResponse Thông tin người dùng sau khi đã cập nhật
     */
    @Transactional
    public UserResponse updateProfile(UUID currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Cập nhật thông tin người dùng
        userMapper.updateUserFromRequest(request, user);

        // Lưu lại thông tin mới vào database
        User updatedUser = userRepository.save(user);

        return userMapper.toUserResponse(updatedUser);
    }
}
