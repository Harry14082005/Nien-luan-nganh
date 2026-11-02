package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.UserPresenceResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPresenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private UserPresenceService userPresenceService;

    private UUID userId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .lastSeenAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    @DisplayName("Kết nối session đầu tiên: Broadcast trạng thái Online")
    void testAddSession_FirstSession_BroadcastsOnline() {
        String sessionId = "session-tab-1";

        userPresenceService.addSession(userId, sessionId);

        assertTrue(userPresenceService.isUserOnline(userId));
        assertEquals(1, userPresenceService.getSessionCount(userId));

        // Kiểm tra broadcast qua 2 kênh: chung và riêng
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/users.presence"), any(UserPresenceResponse.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/users." + userId + ".presence"), any(UserPresenceResponse.class));
    }

    @Test
    @DisplayName("Bẫy nhiều tab: Mở tab thứ 2 không broadcast lại Online")
    void testAddSession_MultiSession_DoesNotBroadcastAgain() {
        String session1 = "session-tab-1";
        String session2 = "session-tab-2";

        userPresenceService.addSession(userId, session1);
        userPresenceService.addSession(userId, session2);

        assertTrue(userPresenceService.isUserOnline(userId));
        assertEquals(2, userPresenceService.getSessionCount(userId));

        // Chỉ broadcast 1 lần ở session đầu tiên
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/users.presence"), any(UserPresenceResponse.class));
    }

    @Test
    @DisplayName("Bẫy nhiều tab: Đóng 1 trong 2 tab vẫn giữ Online, không broadcast Offline")
    void testRemoveSession_MultiSession_RemainsOnline() {
        String session1 = "session-tab-1";
        String session2 = "session-tab-2";

        userPresenceService.addSession(userId, session1);
        userPresenceService.addSession(userId, session2);

        // Đóng tab 1
        userPresenceService.removeSession(session1, null);

        // Vẫn còn tab 2 -> Vẫn Online
        assertTrue(userPresenceService.isUserOnline(userId));
        assertEquals(1, userPresenceService.getSessionCount(userId));

        // Không gọi cập nhật DB
        verify(userRepository, never()).updateLastSeenAt(any(), any());
    }

    @Test
    @DisplayName("Đóng toàn bộ tab: Cập nhật lastSeenAt vào DB và broadcast Offline")
    void testRemoveSession_LastSession_TransitionsToOfflineAndUpdatesLastSeenAt() {
        String session1 = "session-tab-1";

        userPresenceService.addSession(userId, session1);
        reset(messagingTemplate); // reset để theo dõi lượt gọi khi disconnect

        // Đóng session duy nhất
        userPresenceService.removeSession(session1, null);

        assertFalse(userPresenceService.isUserOnline(userId));
        assertEquals(0, userPresenceService.getSessionCount(userId));

        // Cập nhật lastSeenAt vào DB
        verify(userRepository, times(1)).updateLastSeenAt(eq(userId), any(LocalDateTime.class));

        // Broadcast offline
        ArgumentCaptor<UserPresenceResponse> captor = ArgumentCaptor.forClass(UserPresenceResponse.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/users.presence"), captor.capture());

        UserPresenceResponse response = captor.getValue();
        assertEquals(userId, response.getUserId());
        assertFalse(response.isOnline());
        assertNotNull(response.getLastSeenAt());
    }

    @Test
    @DisplayName("Lấy chi tiết hiện diện người dùng đang Online")
    void testGetUserPresence_OnlineUser() {
        userPresenceService.addSession(userId, "session-1");
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserPresenceResponse presence = userPresenceService.getUserPresence(userId);

        assertNotNull(presence);
        assertEquals(userId, presence.getUserId());
        assertTrue(presence.isOnline());
        assertNull(presence.getLastSeenAt()); // Đang online thì lastSeenAt là null
    }

    @Test
    @DisplayName("Lấy chi tiết hiện diện người dùng đang Offline")
    void testGetUserPresence_OfflineUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserPresenceResponse presence = userPresenceService.getUserPresence(userId);

        assertNotNull(presence);
        assertEquals(userId, presence.getUserId());
        assertFalse(presence.isOnline());
        assertEquals(mockUser.getLastSeenAt(), presence.getLastSeenAt());
    }

    @Test
    @DisplayName("Lấy danh sách hiện diện hàng loạt (Batch Presence)")
    void testGetUsersPresence_Batch() {
        UUID user2Id = UUID.randomUUID();
        User user2 = User.builder().userId(user2Id).username("user2").build();

        userPresenceService.addSession(userId, "session-user1"); // user 1 online
        // user 2 offline

        when(userRepository.findAllById(any())).thenReturn(List.of(mockUser, user2));

        List<UserPresenceResponse> result = userPresenceService.getUsersPresence(List.of(userId, user2Id));

        assertEquals(2, result.size());
        UserPresenceResponse res1 = result.stream().filter(r -> r.getUserId().equals(userId)).findFirst().orElseThrow();
        UserPresenceResponse res2 = result.stream().filter(r -> r.getUserId().equals(user2Id)).findFirst().orElseThrow();

        assertTrue(res1.isOnline());
        assertFalse(res2.isOnline());
    }

    @Test
    @DisplayName("Batch Presence: Tự động khử trùng lặp ID trước khi truy vấn DB")
    void testGetUsersPresence_Deduplication() {
        // Gửi danh sách có ID trùng lặp 3 lần
        List<UUID> duplicateIds = List.of(userId, userId, userId);

        when(userRepository.findAllById(any())).thenReturn(List.of(mockUser));

        List<UserPresenceResponse> result = userPresenceService.getUsersPresence(duplicateIds);

        assertEquals(1, result.size());
        // Xác minh userRepository.findAllById chỉ nhận collection có 1 phần tử duy nhất sau khi khử trùng
        verify(userRepository, times(1)).findAllById(argThat(ids -> ids instanceof Collection<?> c && c.size() == 1));
    }
}
