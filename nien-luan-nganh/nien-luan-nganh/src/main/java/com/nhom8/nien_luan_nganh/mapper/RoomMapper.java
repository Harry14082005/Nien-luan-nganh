package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomListResponse toRoomResponse(Room room);

    Room toRoom(CreateGroupRequest request);
    Room toRoom(CreateDirectRoomRequest request);
}
