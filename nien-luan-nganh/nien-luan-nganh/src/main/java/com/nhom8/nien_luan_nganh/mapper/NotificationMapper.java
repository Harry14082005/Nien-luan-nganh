package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.NotificationResponse;
import com.nhom8.nien_luan_nganh.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "actor.userId", target = "actorId")
    @Mapping(source = "actor.username", target = "actorUsername")
    @Mapping(source = "actor.avatarUrl", target = "actorAvatarUrl")
    NotificationResponse toNotificationResponse(Notification notification);

    List<NotificationResponse> toNotificationResponseList(List<Notification> notifications);
}
