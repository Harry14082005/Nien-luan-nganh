package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.entity.RoomMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMemberMapper {
    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "userId", source = "user.id")
    // @Mapping(target = "roomId", source = "room.roomId")
    // @Mapping(target = "userId", source = "user.userId")
    RoomMemberResponse toRoomMemberResponse(RoomMember roomMember);
}
