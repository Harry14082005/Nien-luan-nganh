package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.ChangePasswordRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateEmailRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdatePhoneRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateProfileRequest;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến Hồ sơ Người dùng (Users
 * Module):
 * - Lấy thông tin cá nhân của người đang đăng nhập (/users/me).
 * - Cập nhật thông tin cá nhân (bio, avatar, tên, giới tính, ngôn ngữ).
 * - Đổi mật khẩu, đổi email, đổi số điện thoại.
 * - Tìm kiếm người dùng và xem trang cá nhân người khác.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Nghiệp vụ 1: Lấy thông tin hồ sơ của chính người dùng đang đăng nhập.
     *
     * @param currentUserId Khóa chính UUID của người dùng trích xuất từ Token
     * @return UserResponse Thông tin người dùng an toàn (không có password)
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    /**
     * Nghiệp vụ 2: Cập nhật thông tin hồ sơ cá nhân.
     * Sử dụng MapStruct để cập nhật tự động các trường có dữ liệu, bỏ qua các
     * trường null.
     *
     * @param currentUserId Khóa chính UUID của người dùng
     * @param request       Dữ liệu cập nhật từ client
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

    /**
     * Nghiệp vụ 3: Đổi mật khẩu tài khoản.
     * Quy trình:
     * 1. Kiểm tra mật khẩu mới và xác nhận mật khẩu mới có khớp nhau không.
     * 2. Đối soát mật khẩu cũ gửi lên với mật khẩu băm trong database.
     * 3. Băm mật khẩu mới bằng BCrypt và lưu vào database.
     *
     * @param currentUserId Khóa chính UUID của người dùng
     * @param request       Chứa oldPassword, newPassword, confirmNewPassword
     */

    @Transactional
    public void changePassword(UUID currentUserId, ChangePasswordRequest request) {
        // 1. Kiem tra xac nhan mat khau moi
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        // 2. Kiem tra mat khau cu co dung hong
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }
        // Kiểm tra mật khẩu mới không được trùng với mật khẩu cũ
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        // 3. Bam mat khau moi va luu lai
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Nghiệp vụ 4: Cập nhật địa chỉ Email mới.
     * Quy trình:
     * 1. Kiểm tra email mới nếu trùng với email hiện tại thì không cần làm gì.
     * 2. Kiểm tra email mới đã được tài khoản khác đăng ký chưa (tránh trùng lặp).
     * 3. Cập nhật và lưu vào CSDL.
     *
     * @param currentUserId Khóa chính UUID của người dùng
     * @param request       Chứa email mới
     * @return UserResponse Thông tin người dùng sau khi cập nhật
     */
    @Transactional
    public UserResponse updateEmail(UUID currentUserId, UpdateEmailRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String newEmail = request.getEmail().trim();

        // Nếu trùng với email hiện tại của chính họ thì bỏ qua
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            return userMapper.toUserResponse(user);
        }

        // Kiểm tra xem đã có ai dùng email này chưa
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    /**
     * Nghiệp vụ 5: Cập nhật Số điện thoại mới.
     * Quy trình:
     * 1. Kiểm tra số điện thoại mới nếu trùng với SĐT hiện tại thì bỏ qua.
     * 2. Kiểm tra SĐT mới đã được tài khoản khác đăng ký chưa.
     * 3. Cập nhật và lưu vào CSDL.
     *
     * @param currentUserId Khóa chính UUID của người dùng
     * @param request       Chứa phoneNumber mới
     * @return UserResponse Thông tin người dùng sau khi cập nhật
     */
    @Transactional
    public UserResponse updatePhone(UUID currentUserId, UpdatePhoneRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String newPhone = request.getPhoneNumber().trim();

        // Nếu trùng với số điện thoại hiện tại thì bỏ qua
        if (newPhone.equals(user.getPhoneNumber())) {
            return userMapper.toUserResponse(user);
        }

        // Kiểm tra xem đã có ai dùng số điện thoại này chưa
        if (userRepository.existsByPhoneNumber(newPhone)) {
            throw new AppException(ErrorCode.PHONE_NUMBER_EXISTED);
        }

        user.setPhoneNumber(newPhone);
        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    /**
     * Nghiệp vụ 6: Tìm kiếm người dùng theo từ khóa (Email, Số điện thoại hoặc Tên
     * hiển thị).
     * Loại bỏ các tài khoản đã bị khóa (banned) và loại trừ chính người đang tìm
     * kiếm.
     * Hỗ trợ phân trang (Pageable) tối ưu bộ nhớ và trải nghiệm người dùng.
     *
     * @param query         Từ khóa tìm kiếm (tham số q)
     * @param currentUserId Khóa chính UUID của người đang thực hiện tìm kiếm
     * @param pageable      Thông tin phân trang (page, size, sort)
     * @return PageResponse<UserResponse> Dữ liệu phân trang kèm danh sách người
     *         dùng phù hợp
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String query, UUID currentUserId, Pageable pageable) {
        if (query == null || query.trim().isBlank()) {
            return PageResponse.empty(pageable.getPageNumber(), pageable.getPageSize());
        }

        Page<User> users = userRepository.searchUsers(query.trim(), currentUserId, pageable);
        Page<UserResponse> userResponses = users.map(userMapper::toUserResponse);
        return PageResponse.from(userResponses);
    }

    /**
     * Nghiệp vụ 7: Xem trang cá nhân công khai của người dùng khác theo ID.
     *
     * @param userId Khóa chính UUID của người dùng cần xem
     * @return UserResponse Thông tin hồ sơ người dùng
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }
}
