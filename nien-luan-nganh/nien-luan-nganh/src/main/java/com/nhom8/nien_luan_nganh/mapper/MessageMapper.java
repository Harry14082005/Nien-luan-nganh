package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.MessageHistoryResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.dto.response.ReactionResponse;
import com.nhom8.nien_luan_nganh.dto.response.RepliedMessageResponse;
import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.entity.MessageHistory;
import com.nhom8.nien_luan_nganh.entity.MessageReaction;
import com.nhom8.nien_luan_nganh.enums.ReactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Mapper chuyển đổi giữa Message, MessageHistory, MessageReaction Entity và các DTO Response tương ứng.
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface MessageMapper {

    @Mapping(target = "roomId", source = "room.roomId")
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "repliedMessage", source = "repliedMessage")
    @Mapping(target = "content", expression = "java(Boolean.TRUE.equals(message.getIsRevoked()) ? \"Tin nhắn đã bị thu hồi\" : message.getContent())")
    @Mapping(target = "reactions", source = "messageReactions")
    MessageResponse toMessageResponse(Message message);

    @Mapping(target = "messageId", source = "messageId")
    @Mapping(target = "senderId", source = "sender.userId")
    @Mapping(target = "senderUsername", source = "sender.username")
    @Mapping(target = "content", expression = "java(Boolean.TRUE.equals(message.getIsRevoked()) ? \"Tin nhắn đã bị thu hồi\" : message.getContent())")
    RepliedMessageResponse toRepliedMessageResponse(Message message);

    @Mapping(target = "messageId", source = "message.messageId")
    MessageHistoryResponse toMessageHistoryResponse(MessageHistory history);

    List<MessageHistoryResponse> toMessageHistoryResponseList(List<MessageHistory> histories);

    List<MessageResponse> toMessageResponseList(List<Message> messages);

    @Mapping(target = "messageId", source = "messageReactionId.messageId")
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "reactionType", source = "reactionType", qualifiedByName = "mapReactionType")
    ReactionResponse toReactionResponse(MessageReaction reaction);

    List<ReactionResponse> toReactionResponseList(List<MessageReaction> reactions);

    @Named("mapReactionType")
    default ReactionType mapReactionType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return ReactionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

