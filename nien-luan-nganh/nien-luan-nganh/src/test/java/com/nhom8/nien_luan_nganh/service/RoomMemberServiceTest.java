package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.MessageReceiptResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.MessageReceiptType;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomMemberServiceTest {

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private RoomMemberService roomMemberService;

    private UUID roomId;
    private UUID userId;
    private UUID message1Id;
    private UUID message2Id;
    private Room mockRoom;
    private User mockUser;
    private RoomMember mockMember;
    private Message mockMessage1;
    private Message mockMessage2;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        userId = UUID.randomUUID();
        message1Id = UUID.randomUUID();
        message2Id = UUID.randomUUID();

        mockRoom = Room.builder()
                .roomId(roomId)
                .lastMessageId(message2Id)
                .build();

        mockUser = User.builder()
                .userId(userId)
                .username("harry")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        mockMember = RoomMember.builder()
                .room(mockRoom)
                .user(mockUser)
                .roomMemberId(new RoomMemberId(roomId, userId))
                .status(RoomMemberStatus.ACTIVE)
                .roomMemberRole(RoomRole.MEMBER)
                .lastDeliveredMessageId(null)
                .lastReadMessageId(null)
                .build();

        LocalDateTime now = LocalDateTime.now();
        mockMessage1 = Message.builder()
                .messageId(message1Id)
                .room(mockRoom)
                .sender(mockUser)
                .createdAt(now.minusMinutes(5))
                .build();

        mockMessage2 = Message.builder()
                .messageId(message2Id)
                .room(mockRoom)
                .sender(mockUser)
                .createdAt(now)
                .build();
    }

    @Test
    @DisplayName("Đánh dấu đã nhận: Cập nhật thành công khi tin nhắn mới hơn")
    void testMarkAsDelivered_Success_WhenNewer() {
        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.of(mockMember));
        when(messageRepository.findById(message1Id))
                .thenReturn(Optional.of(mockMessage1));
        when(roomMemberRepository.save(any(RoomMember.class)))
                .thenReturn(mockMember);

        MessageReceiptResponse response = roomMemberService.markAsDelivered(roomId, message1Id, userId);

        assertNotNull(response);
        assertEquals(roomId, response.getRoomId());
        assertEquals(userId, response.getUserId());
        assertEquals(message1Id, response.getMessageId());
        assertEquals(MessageReceiptType.DELIVERED, response.getReceiptType());
        assertEquals(message1Id, mockMember.getLastDeliveredMessageId());
        verify(roomMemberRepository, times(1)).save(mockMember);
    }

    @Test
    @DisplayName("Đánh dấu đã nhận: Bỏ qua khi tin nhắn gửi lên cũ hơn mốc hiện tại")
    void testMarkAsDelivered_Ignored_WhenOlder() {
        // Gán mốc hiện tại là message2 (mới hơn message1)
        mockMember.setLastDeliveredMessageId(message2Id);

        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.of(mockMember));
        when(messageRepository.findById(message1Id))
                .thenReturn(Optional.of(mockMessage1));
        when(messageRepository.findById(message2Id))
                .thenReturn(Optional.of(mockMessage2));

        MessageReceiptResponse response = roomMemberService.markAsDelivered(roomId, message1Id, userId);

        assertNull(response);
        assertEquals(message2Id, mockMember.getLastDeliveredMessageId());
        verify(roomMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đánh dấu đã nhận: Bỏ qua ngay lập tức khi trùng với mốc hiện tại (Watermark short-circuit)")
    void testMarkAsDelivered_Ignored_WhenSameMessageId() {
        mockMember.setLastDeliveredMessageId(message1Id);

        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.of(mockMember));
        when(messageRepository.findById(message1Id))
                .thenReturn(Optional.of(mockMessage1));

        MessageReceiptResponse response = roomMemberService.markAsDelivered(roomId, message1Id, userId);

        assertNull(response);
        assertEquals(message1Id, mockMember.getLastDeliveredMessageId());
        verify(roomMemberRepository, never()).save(any());
        // Chỉ gọi findById 1 lần để lấy message hiện tại, không gọi thêm lần 2 cho mốc cũ vì trùng ID
        verify(messageRepository, times(1)).findById(message1Id);
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: Cập nhật cả lastReadMessageId và lastDeliveredMessageId")
    void testMarkAsRead_Success_WhenNewer() {
        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.of(mockMember));
        when(messageRepository.findById(message2Id))
                .thenReturn(Optional.of(mockMessage2));
        when(roomMemberRepository.save(any(RoomMember.class)))
                .thenReturn(mockMember);

        MessageReceiptResponse response = roomMemberService.markAsRead(roomId, message2Id, userId);

        assertNotNull(response);
        assertEquals(roomId, response.getRoomId());
        assertEquals(userId, response.getUserId());
        assertEquals(message2Id, response.getMessageId());
        assertEquals(MessageReceiptType.READ, response.getReceiptType());
        assertEquals(message2Id, mockMember.getLastReadMessageId());
        assertEquals(message2Id, mockMember.getLastDeliveredMessageId());
        verify(roomMemberRepository, times(1)).save(mockMember);
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: Tự động lấy tin nhắn mới nhất của phòng khi messageId null")
    void testMarkAsRead_AutoResolveLatestMessage_WhenMessageIdNull() {
        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.of(mockMember));
        when(roomRepository.findById(roomId))
                .thenReturn(Optional.of(mockRoom));
        when(messageRepository.findById(message2Id))
                .thenReturn(Optional.of(mockMessage2));
        when(roomMemberRepository.save(any(RoomMember.class)))
                .thenReturn(mockMember);

        MessageReceiptResponse response = roomMemberService.markAsRead(roomId, null, userId);

        assertNotNull(response);
        assertEquals(message2Id, response.getMessageId());
        assertEquals(message2Id, mockMember.getLastReadMessageId());
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: Báo lỗi khi người dùng không phải thành viên phòng")
    void testMarkAsRead_ThrowsException_WhenNotMember() {
        when(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> roomMemberService.markAsRead(roomId, message1Id, userId));

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, exception.getErrorCode());
    }
}

