package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    RoomRepository roomRepository;

    UserRepository userRepository;

    RoomMapper roomMapper;

    //tạo group
    @Transactional
    public RoomResponse createGroup(CreateGroupRequest request, UUID currentUserId){
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomMapper.toRoom(request);
        room.setRoomType(RoomType.GROUP);
        room.setCreatedBy(creator);

        roomRepository.save(room);

        /// xử lý thêm danh sách thành viên

        return roomMapper.toRoomResponse(room);
    }

    @Transactional
    public RoomResponse createDirectRoom(CreateDirectRoomRequest request, UUID currentUserId){
        if(request.getUserId() == null)
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        if(request.getUserId().equals(currentUserId))
            throw  new AppException(ErrorCode.CANNOT_ADD_YOURSELF);

        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        //xử lý thêm người được thêm vô phòng chat

        // Đã có phòng direct giữa 2 người thì trả về phòng cũ
        return roomRepository.findDirectRoom(currentUserId, request.getUserId(), RoomType.DIRECT)
                .map(roomMapper::toRoomResponse)
                .orElseGet(() -> {
                    Room room = roomMapper.toRoom(request);
                    room.setRoomType(RoomType.DIRECT);
                    roomRepository.save(room);

                    // xử lý thêm thành viên

                    return roomMapper.toRoomResponse(room);
                });
    }
}
