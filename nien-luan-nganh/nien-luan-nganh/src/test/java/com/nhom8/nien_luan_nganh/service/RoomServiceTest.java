package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomMemberService roomMemberService;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private RoomService roomService;

    private UUID currentUserId;
    private UUID peerUserId;
    private User currentUser;
    private User peerUser;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        peerUserId = UUID.randomUUID();

        currentUser = User.builder()
                .userId(currentUserId)
                .username("me_user")
                .avatarUrl("me.png")
                .build();

        peerUser = User.builder()
                .userId(peerUserId)
                .username("hermione")
                .avatarUrl("hermione.png")
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách phòng chat (Phương án A): đầy đủ tên phòng DIRECT, preview tin nhắn cuối, hasUnread và unreadCount")
    void getRooms_ReturnsPreviewAndUnreadStatus() {
        UUID groupRoomId = UUID.randomUUID();
        UUID directRoomId = UUID.randomUUID();

        Room groupRoom = Room.builder()
                .roomId(groupRoomId)
                .roomName("Nhóm 8 Niên luận")
                .roomAvatarURL("group.png")
                .roomType(RoomType.GROUP)
                .updatedAt(LocalDateTime.now())
                .build();

        Room directRoom = Room.builder()
                .roomId(directRoomId)
                .roomName(null)
                .roomAvatarURL(null)
                .roomType(RoomType.DIRECT)
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        RoomMember groupMembership = RoomMember.builder()
                .roomMemberId(new RoomMemberId(groupRoomId, currentUserId))
                .room(groupRoom)
                .user(currentUser)
                .roomMemberRole(RoomRole.ADMIN)
                .status(RoomMemberStatus.ACTIVE)
                .build();

        RoomMember directMembership = RoomMember.builder()
                .roomMemberId(new RoomMemberId(directRoomId, currentUserId))
                .room(directRoom)
                .user(currentUser)
                .roomMemberRole(RoomRole.MEMBER)
                .status(RoomMemberStatus.ACTIVE)
                .build();

        RoomMember peerDirectMembership = RoomMember.builder()
                .roomMemberId(new RoomMemberId(directRoomId, peerUserId))
                .room(directRoom)
                .user(peerUser)
                .roomMemberRole(RoomRole.MEMBER)
                .status(RoomMemberStatus.ACTIVE)
                .build();

        Message latestGroupMsg = Message.builder()
                .messageId(UUID.randomUUID())
                .room(groupRoom)
                .sender(peerUser)
                .messageType(MessageType.TEXT)
                .content("Tối nay họp lúc 8h nhé!")
                .isRevoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        Page<Room> roomPage = new PageImpl<>(List.of(groupRoom, directRoom));

        when(userRepository.existsById(currentUserId)).thenReturn(true);
        when(roomRepository.findDistinctByRoomMembers_User_UserId(eq(currentUserId), any(Pageable.class)))
                .thenReturn(roomPage);
        when(roomMapper.toRoomListResponse(groupRoom)).thenReturn(
                RoomListResponse.builder()
                        .roomId(groupRoomId)
                        .roomType(RoomType.GROUP)
                        .roomName("Nhóm 8 Niên luận")
                        .roomAvatarURL("group.png")
                        .build()
        );
        when(roomMapper.toRoomListResponse(directRoom)).thenReturn(
                RoomListResponse.builder()
                        .roomId(directRoomId)
                        .roomType(RoomType.DIRECT)
                        .roomName(null)
                        .roomAvatarURL(null)
                        .build()
        );
        when(roomMemberRepository.findByRoomIdsAndUserId(anyList(), eq(currentUserId)))
                .thenReturn(List.of(groupMembership, directMembership));
        when(roomMemberRepository.findOtherMembersInRooms(anyList(), eq(currentUserId)))
                .thenReturn(List.of(peerDirectMembership));
        when(messageRepository.findLatestMessagesByRoomIds(anyList()))
                .thenReturn(List.of(latestGroupMsg));
        when(messageRepository.countUnreadMessagesByRoomIds(anyList(), eq(currentUserId)))
                .thenReturn(Collections.singletonList(new Object[]{groupRoomId, 3L}));

        PageResponse<RoomListResponse> pageResponse = roomService.getRooms(currentUserId, null, 0, 20);
        List<RoomListResponse> result = pageResponse.getContent();

        assertEquals(2, result.size());

        // Kiểm tra phòng nhóm (có 3 tin chưa đọc -> in đậm)
        RoomListResponse groupItem = result.get(0);
        assertEquals(groupRoomId, groupItem.getRoomId());
        assertEquals("Nhóm 8 Niên luận", groupItem.getRoomName());
        assertEquals("Tối nay họp lúc 8h nhé!", groupItem.getLastMessageContent());
        assertEquals("hermione", groupItem.getLastMessageSenderName());
        assertTrue(groupItem.isHasUnread());
        assertEquals(3L, groupItem.getUnreadCount());

        // Kiểm tra phòng 1-1 DIRECT (lấy tên & avatar của đối phương, không có tin chưa đọc)
        RoomListResponse directItem = result.get(1);
        assertEquals(directRoomId, directItem.getRoomId());
        assertEquals("hermione", directItem.getRoomName());
        assertEquals("hermione.png", directItem.getRoomAvatarURL());
        assertFalse(directItem.isHasUnread());
        assertEquals(0L, directItem.getUnreadCount());
    }
}

