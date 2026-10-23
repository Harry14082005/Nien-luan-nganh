package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.request.UpdateProfileRequest;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.User;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Interface Mapper sử dụng MapStruct để chuyển đổi tự động giữa Entity User và DTO UserResponse.
 * Cấu hình componentModel = "spring" để MapStruct tự động đăng ký Bean trong Spring Context,
 * cho phép tiêm (inject) trực tiếp vào các Service.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UpdateProfileRequest request, @MappingTarget User user);
}
