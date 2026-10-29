package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.SendMessageRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateMessageRequest;
import com.nhom8.nien_luan_nganh.dto.response.CursorResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageHistoryResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.MessageMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho MessageService:
 * 1. Gửi tin nhắn Text, Image, File và Reply.
 * 2. Chỉnh sửa tin nhắn và lưu lịch sử vào message_histories.
 * 3. Thu hồi tin nhắn (Người gửi tự thu hồi hoặc Owner/Admin duyệt).
 * 4. Xem lịch sử chỉnh sửa tin nhắn.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private MessageHistoryRepository messageHistoryRepository;

    @Mock
    private MessageReactionRepository messageReactionRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageService messageService;

    private UUID userId;
    private UUID otherUserId;
    private UUID roomId;
    private UUID messageId;
    private User user;
    private Room room;
    private Message message;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        user = User.builder()
                .userId(userId)
                .username("harry")
                .build();

        room = Room.builder()
                .roomId(roomId)
                .roomType(RoomType.GROUP)
                .build();

        message = Message.builder()
                .messageId(messageId)
                .room(room)
                .sender(user)
                .messageType(MessageType.TEXT)
                .content("Nội dung ban đầu")
                .isEdited(false)
                .isRevoked(false)
                .build();
    }

    @Test
    @DisplayName("Gửi tin nhắn thành công trong nhóm chat")
    void sendMessage_Success() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Xin chào cả nhóm")
                .messageType(MessageType.TEXT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .content("Xin chào cả nhóm")
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.sendMessage(userId, roomId, request);

        assertNotNull(response);
        assertEquals("Xin chào cả nhóm", response.getContent());
        verify(roomRepository, times(1)).save(room);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Gửi tin nhắn thất bại khi không phải là thành viên trong phòng")
    void sendMessage_NotMember_ThrowsException() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Chào")
                .messageType(MessageType.TEXT)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () ->
                messageService.sendMessage(userId, roomId, request));

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, exception.getErrorCode());
    }

    @Test
    @DisplayName("Chỉnh sửa tin nhắn thành công và lưu lịch sử")
    void updateMessage_Success() {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("Nội dung đã được chỉnh sửa")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .content("Nội dung đã được chỉnh sửa")
                .isEdited(true)
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.updateMessage(userId, roomId, messageId, request);

        assertNotNull(response);
        assertEquals("Nội dung đã được chỉnh sửa", response.getContent());
        assertTrue(response.getIsEdited());
        verify(messageHistoryRepository, times(1)).save(any(MessageHistory.class));
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Chỉnh sửa tin nhắn thất bại khi người sửa không phải người gửi")
    void updateMessage_NotSender_ThrowsException() {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("Hack tin nhắn")
                .build();

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(new User()));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(otherUserId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.updateMessage(otherUserId, roomId, messageId, request));

        assertEquals(ErrorCode.CANNOT_EDIT_MESSAGE, exception.getErrorCode());
        verify(messageHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chỉnh sửa tin nhắn thất bại khi tin nhắn đã bị thu hồi")
    void updateMessage_AlreadyRevoked_ThrowsException() {
        message.setIsRevoked(true);
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("Sửa tin thu hồi")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.updateMessage(userId, roomId, messageId, request));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Người gửi tự thu hồi tin nhắn thành công")
    void revokeMessage_BySender_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .content("Tin nhắn đã bị thu hồi")
                .isRevoked(true)
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.revokeMessage(userId, roomId, messageId);

        assertNotNull(response);
        assertTrue(response.getIsRevoked());
        assertEquals("Tin nhắn đã bị thu hồi", response.getContent());
        assertNull(message.getContent()); // Nội dung gốc bị xóa khỏi entity
        assertTrue(message.getIsRevoked());
        assertNotNull(message.getRevokedAt());
        verify(messageHistoryRepository, times(1)).deleteByMessage_MessageId(messageId);
    }

    @Test
    @DisplayName("Không phải người gửi không được quyền thu hồi tin nhắn của người khác")
    void revokeMessage_NotSender_ThrowsException() {
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(new User()));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(otherUserId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.revokeMessage(otherUserId, roomId, messageId));

        assertEquals(ErrorCode.CANNOT_REVOKE_MESSAGE, exception.getErrorCode());
        assertFalse(message.getIsRevoked());
    }

    @Test
    @DisplayName("Lấy lịch sử chỉnh sửa tin nhắn thành công")
    void getMessageHistory_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        MessageHistory history = MessageHistory.builder()
                .message(message)
                .content("Bản ghi lịch sử trước đó")
                .editedAt(LocalDateTime.now())
                .build();
        when(messageHistoryRepository.findHistoryByMessageId(messageId)).thenReturn(List.of(history));

        MessageHistoryResponse historyResponse = MessageHistoryResponse.builder()
                .messageId(messageId)
                .content("Bản ghi lịch sử trước đó")
                .build();
        when(messageMapper.toMessageHistoryResponseList(anyList())).thenReturn(List.of(historyResponse));

        List<MessageHistoryResponse> responses = messageService.getMessageHistory(userId, roomId, messageId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Bản ghi lịch sử trước đó", responses.get(0).getContent());
    }

    @Test
    @DisplayName("Không được xem lịch sử chỉnh sửa của tin nhắn đã bị thu hồi")
    void getMessageHistory_RevokedMessage_ThrowsException() {
        message.setIsRevoked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.getMessageHistory(userId, roomId, messageId));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
        verify(messageHistoryRepository, never()).findHistoryByMessageId(any());
    }

    @Test
    @DisplayName("Gửi tin nhắn ảnh với content null không bị crash NullPointerException")
    void sendMessage_ImageWithNullContent_Success() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content(null)
                .messageType(MessageType.IMAGE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageType(MessageType.IMAGE)
                .content(null)
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.sendMessage(userId, roomId, request);

        assertNotNull(response);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("Chỉnh sửa tin nhắn thất bại khi nội dung trống")
    void updateMessage_BlankContent_ThrowsException() {
        UpdateMessageRequest request = UpdateMessageRequest.builder()
                .content("   ")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.updateMessage(userId, roomId, messageId, request));

        assertEquals(ErrorCode.MESSAGE_CONTENT_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ghim tin nhắn thành công")
    void pinMessage_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .isPinned(true)
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.pinMessage(userId, roomId, messageId);

        assertNotNull(response);
        assertTrue(response.getIsPinned());
        assertTrue(message.getIsPinned());
        verify(messageRepository, times(1)).save(message);
    }

    @Test
    @DisplayName("Ghim tin nhắn thất bại khi tin nhắn đã bị thu hồi")
    void pinMessage_RevokedMessage_ThrowsException() {
        message.setIsRevoked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class, () ->
                messageService.pinMessage(userId, roomId, messageId));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
        assertFalse(message.getIsPinned());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bỏ ghim tin nhắn thành công")
    void unpinMessage_Success() {
        message.setIsPinned(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .isPinned(false)
                .build();
        when(messageMapper.toMessageResponse(any(Message.class))).thenReturn(expectedResponse);

        MessageResponse response = messageService.unpinMessage(userId, roomId, messageId);

        assertNotNull(response);
        assertFalse(response.getIsPinned());
        assertFalse(message.getIsPinned());
        verify(messageRepository, times(1)).save(message);
    }

    @Test
    @DisplayName("Lấy danh sách tin nhắn đã ghim trong phòng thành công")
    void getPinnedMessages_Success() {
        message.setIsPinned(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findPinnedMessagesByRoomId(roomId)).thenReturn(List.of(message));

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .isPinned(true)
                .build();
        when(messageMapper.toMessageResponseList(anyList())).thenReturn(List.of(expectedResponse));

        List<MessageResponse> responses = messageService.getPinnedMessages(userId, roomId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).getIsPinned());
    }

    @Test
    @DisplayName("Thả cảm xúc mới vào tin nhắn thành công")
    void reactMessage_NewReaction_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageReactionRepository.findById(any())).thenReturn(Optional.empty());
        when(messageReactionRepository.save(any(MessageReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.nhom8.nien_luan_nganh.dto.response.ReactionResponse expectedResponse =
                com.nhom8.nien_luan_nganh.dto.response.ReactionResponse.builder()
                        .messageId(messageId)
                        .userId(userId)
                        .username("harry")
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LOVE)
                        .build();
        when(messageMapper.toReactionResponse(any(MessageReaction.class))).thenReturn(expectedResponse);

        com.nhom8.nien_luan_nganh.dto.request.ReactionRequest request =
                com.nhom8.nien_luan_nganh.dto.request.ReactionRequest.builder()
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LOVE)
                        .build();

        com.nhom8.nien_luan_nganh.dto.response.ReactionResponse response =
                messageService.reactMessage(userId, roomId, messageId, request);

        assertNotNull(response);
        assertEquals(com.nhom8.nien_luan_nganh.enums.ReactionType.LOVE, response.getReactionType());
        verify(messageReactionRepository, times(1)).save(any(MessageReaction.class));
    }

    @Test
    @DisplayName("Cập nhật loại cảm xúc đã thả trước đó thành công")
    void reactMessage_UpdateExistingReaction_Success() {
        MessageReaction existingReaction = MessageReaction.builder()
                .messageReactionId(new MessageReactionId(messageId, userId))
                .message(message)
                .user(user)
                .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LIKE.name())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageReactionRepository.findById(any())).thenReturn(Optional.of(existingReaction));
        when(messageReactionRepository.save(any(MessageReaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.nhom8.nien_luan_nganh.dto.response.ReactionResponse expectedResponse =
                com.nhom8.nien_luan_nganh.dto.response.ReactionResponse.builder()
                        .messageId(messageId)
                        .userId(userId)
                        .username("harry")
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.HAHA)
                        .build();
        when(messageMapper.toReactionResponse(any(MessageReaction.class))).thenReturn(expectedResponse);

        com.nhom8.nien_luan_nganh.dto.request.ReactionRequest request =
                com.nhom8.nien_luan_nganh.dto.request.ReactionRequest.builder()
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.HAHA)
                        .build();

        com.nhom8.nien_luan_nganh.dto.response.ReactionResponse response =
                messageService.reactMessage(userId, roomId, messageId, request);

        assertNotNull(response);
        assertEquals(com.nhom8.nien_luan_nganh.enums.ReactionType.HAHA, response.getReactionType());
        assertEquals("HAHA", existingReaction.getReactionType());
        verify(messageReactionRepository, times(1)).save(existingReaction);
    }

    @Test
    @DisplayName("Thả cảm xúc vào tin nhắn đã thu hồi thì báo lỗi MESSAGE_REVOKED")
    void reactMessage_RevokedMessage_ThrowsException() {
        message.setIsRevoked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        com.nhom8.nien_luan_nganh.dto.request.ReactionRequest request =
                com.nhom8.nien_luan_nganh.dto.request.ReactionRequest.builder()
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LIKE)
                        .build();

        AppException exception = assertThrows(AppException.class,
                () -> messageService.reactMessage(userId, roomId, messageId, request));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
        verify(messageReactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Thả cảm xúc khi không phải thành viên phòng chat thì báo lỗi NOT_ROOM_MEMBER")
    void reactMessage_NotMember_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(false);

        com.nhom8.nien_luan_nganh.dto.request.ReactionRequest request =
                com.nhom8.nien_luan_nganh.dto.request.ReactionRequest.builder()
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LIKE)
                        .build();

        AppException exception = assertThrows(AppException.class,
                () -> messageService.reactMessage(userId, roomId, messageId, request));

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, exception.getErrorCode());
    }

    @Test
    @DisplayName("Gỡ cảm xúc tin nhắn thành công")
    void removeReaction_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        messageService.removeReaction(userId, roomId, messageId);

        verify(messageReactionRepository, times(1)).deleteById(any(MessageReactionId.class));
    }

    @Test
    @DisplayName("Gỡ cảm xúc tin nhắn đã bị thu hồi thì báo lỗi MESSAGE_REVOKED")
    void removeReaction_RevokedMessage_ThrowsException() {
        message.setIsRevoked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class,
                () -> messageService.removeReaction(userId, roomId, messageId));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
        verify(messageReactionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Lấy danh sách cảm xúc của tin nhắn thành công")
    void getMessageReactions_Success() {
        MessageReaction reaction = MessageReaction.builder()
                .messageReactionId(new MessageReactionId(messageId, userId))
                .message(message)
                .user(user)
                .reactionType("LOVE")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageReactionRepository.findByMessageIdWithUser(messageId)).thenReturn(List.of(reaction));

        com.nhom8.nien_luan_nganh.dto.response.ReactionResponse expectedResponse =
                com.nhom8.nien_luan_nganh.dto.response.ReactionResponse.builder()
                        .messageId(messageId)
                        .userId(userId)
                        .reactionType(com.nhom8.nien_luan_nganh.enums.ReactionType.LOVE)
                        .build();
        when(messageMapper.toReactionResponseList(anyList())).thenReturn(List.of(expectedResponse));

        List<com.nhom8.nien_luan_nganh.dto.response.ReactionResponse> responses =
                messageService.getMessageReactions(userId, roomId, messageId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(com.nhom8.nien_luan_nganh.enums.ReactionType.LOVE, responses.get(0).getReactionType());
    }

    @Test
    @DisplayName("Xem cảm xúc của tin nhắn đã thu hồi thì báo lỗi MESSAGE_REVOKED")
    void getMessageReactions_RevokedMessage_ThrowsException() {
        message.setIsRevoked(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        AppException exception = assertThrows(AppException.class,
                () -> messageService.getMessageReactions(userId, roomId, messageId));

        assertEquals(ErrorCode.MESSAGE_REVOKED, exception.getErrorCode());
        verify(messageReactionRepository, never()).findByMessageIdWithUser(any());
    }

    @Test
    @DisplayName("Lấy danh sách tin nhắn lần đầu (không có cursor) thành công")
    void getMessages_InitialLoad_WithoutCursor_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);

        Page<Message> messagePage = new PageImpl<>(List.of(message), PageRequest.of(0, 20), 1);
        when(messageRepository.findByRoom_RoomIdOrderByCreatedAtDesc(eq(roomId), any(Pageable.class)))
                .thenReturn(messagePage);

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(messageId)
                .content("Tin nhắn ban đầu")
                .build();
        when(messageMapper.toMessageResponseList(anyList())).thenReturn(List.of(expectedResponse));

        CursorResponse<MessageResponse> response = messageService.getMessages(userId, roomId, null, 20);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertFalse(response.isHasMore());
        assertNull(response.getNextCursor());
    }

    @Test
    @DisplayName("Lấy danh sách tin nhắn cũ hơn (cuộn lên có cursor) thành công")
    void getMessages_ScrollUp_WithCursor_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        Message olderMessage = Message.builder()
                .messageId(UUID.randomUUID())
                .room(room)
                .sender(user)
                .content("Tin nhắn cũ hơn")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        // Giả lập còn trang tiếp theo (totalElements = 5)
        Page<Message> messagePage = new PageImpl<>(List.of(olderMessage), PageRequest.of(0, 1), 5);
        when(messageRepository.findByRoomIdBefore(eq(roomId), eq(messageId), any(Pageable.class)))
                .thenReturn(messagePage);

        MessageResponse expectedResponse = MessageResponse.builder()
                .messageId(olderMessage.getMessageId())
                .content("Tin nhắn cũ hơn")
                .build();
        when(messageMapper.toMessageResponseList(anyList())).thenReturn(List.of(expectedResponse));

        CursorResponse<MessageResponse> response = messageService.getMessages(userId, roomId, messageId, 1);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertTrue(response.isHasMore());
        assertEquals(olderMessage.getMessageId(), response.getNextCursor());
    }

    @Test
    @DisplayName("Lấy tin nhắn với cursor không tồn tại thì báo lỗi MESSAGE_NOT_FOUND")
    void getMessages_CursorNotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(true);
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> messageService.getMessages(userId, roomId, messageId, 20));

        assertEquals(ErrorCode.MESSAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Lấy tin nhắn khi không phải thành viên phòng chat thì báo lỗi NOT_ROOM_MEMBER")
    void getMessages_NotMember_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> messageService.getMessages(userId, roomId, null, 20));

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, exception.getErrorCode());
    }
}

