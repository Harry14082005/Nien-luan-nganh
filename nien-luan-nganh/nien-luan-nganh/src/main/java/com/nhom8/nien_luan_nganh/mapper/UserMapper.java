package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import org.mapstruct.Mapper;

/**
 * Interface Mapper sử dụng MapStruct để chuyển đổi tự động giữa Entity User và DTO UserResponse.
 * Cấu hình componentModel = "spring" để MapStruct tự động đăng ký Bean trong Spring Context,
 * cho phép tiêm (inject) trực tiếp vào các Service.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}
