package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    // @Mapping(target = "roomAvatarURL", source = "avatarRoom")
    // @Mapping(target = "createdBy", source = "createdBy.username")
    RoomResponse toRoomResponse(Room room);

    // @Mapping(target = "avatarRoom", source = "roomAvatarURL")
    Room toRoom(CreateGroupRequest request);

    Room toRoom(CreateDirectRoomRequest request);
}
