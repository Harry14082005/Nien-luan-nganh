package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    //tạo nhóm nhắn tin
    @PostMapping("/group")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> createGroup(
            Authentication authentication,
            @Valid @RequestBody CreateGroupRequest request
            ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomDetailResponse response = roomService.createGroup(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo nhóm thành công"));

    }

    //kết nối trực tiếp //có thể cần xử lý thêm trường hợp lần sau ấn (tại vì nó không tạo mới)
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> createDirectRoom(
            Authentication authentication,
            @Valid @RequestBody CreateDirectRoomRequest request
            ){
        UUID currentUserId = UUID.fromString(authentication.getName());

        RoomDetailResponse response = roomService.createDirectRoom(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo phòng trực tiếp thành công"));
    }
}
