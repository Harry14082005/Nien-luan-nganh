package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "createdBy", source = "createdBy.userId")
    RoomDetailResponse toRoomDetailResponse(Room room);

    @Mapping(target = "lastMessageId", ignore = true)
    @Mapping(target = "lastMessageContent", ignore = true)
    @Mapping(target = "lastMessageSenderName", ignore = true)
    @Mapping(target = "lastMessageAt", ignore = true)
    @Mapping(target = "lastDeliveredMessageId", ignore = true)
    @Mapping(target = "lastReadMessageId", ignore = true)
    RoomListResponse toRoomListResponse(Room room);

    Room toRoom(CreateGroupRequest request);

    Room toRoom(CreateDirectRoomRequest request);
}
