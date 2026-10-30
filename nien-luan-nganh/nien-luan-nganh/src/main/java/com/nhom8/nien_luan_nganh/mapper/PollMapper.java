package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.PollOptionResponse;
import com.nhom8.nien_luan_nganh.dto.response.PollResponse;
import com.nhom8.nien_luan_nganh.entity.Poll;
import com.nhom8.nien_luan_nganh.entity.PollOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PollMapper {
    @Mapping(source = "room.roomId", target = "roomId")
    @Mapping(source = "message.messageId", target = "messageId")
    @Mapping(source = "createdBy.userId", target = "createdById")
    @Mapping(source = "createdBy.username", target = "createdByName")
    PollResponse toPollResponse(Poll poll);

    PollOptionResponse toPollOptionResponse(PollOption option);
}
