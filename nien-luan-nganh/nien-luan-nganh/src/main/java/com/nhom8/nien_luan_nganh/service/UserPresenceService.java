package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.UserPresenceResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý trạng thái Đang online / Vừa mới truy cập (User Presence).
 * Áp dụng giải pháp Multi-session Counter trên RAM (ConcurrentHashMap) để tránh
 * bẫy rớt trạng thái khi người dùng mở nhiều tab hoặc nhiều thiết bị cùng lúc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Lưu danh sách session ID đang hoạt động của từng người dùng
    private final Map<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();

    // Tra cứu ngược từ sessionId sang userId để xử lý ngắt kết nối với độ phức tạp
    // O(1)
    // neu khong co, hethong phai duyet qua tatca user de biet session la cua ai
    private final Map<String, UUID> sessionToUser = new ConcurrentHashMap<>();

    /**
     * Ghi nhận một kết nối WebSocket mới của người dùng.
     * Chỉ broadcast trạng thái Online nếu đây là session đầu tiên (trước đó đang
     * offline).
     * Dong bo hoa da luong voi synchronized
     */
    public synchronized void addSession(UUID userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }

        sessionToUser.put(sessionId, userId);
        Set<String> sessions = userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = sessions.isEmpty();
        sessions.add(sessionId);

        log.info("User {} kết nối session {} (tổng sessions: {})", userId, sessionId, sessions.size());

        if (wasEmpty) {
            // Chuyển từ Offline sang Online -> broadcast
            broadcastPresence(userId, true, null);
        }
    }

    /**
     * Ghi nhận ngắt kết nối WebSocket của một session.
     * Chỉ broadcast trạng thái Offline và cập nhật lastSeenAt khi toàn bộ các tab /
     * thiết bị của user đã đóng (sessions <= 0).
     */
    public synchronized void removeSession(String sessionId, UUID fallbackUserId) {
        if (sessionId == null) {
            return;
        }

        UUID userId = sessionToUser.remove(sessionId);
        if (userId == null) {
            userId = fallbackUserId;
        }

        if (userId == null) {
            log.debug("Session {} ngắt kết nối nhưng không xác định được userId", sessionId);
            return;
        }

        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            log.info("User {} đóng session {} (còn lại: {})", userId, sessionId, sessions.size());

            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                LocalDateTime now = LocalDateTime.now();

                // Cập nhật DB thời gian truy cập lần cuối
                userRepository.updateLastSeenAt(userId, now);

                // Chuyển từ Online sang Offline -> broadcast
                broadcastPresence(userId, false, now);
            }
        }
    }

    /**
     * Kiểm tra nhanh trạng thái trực tuyến của một người dùng.
     */
    public boolean isUserOnline(UUID userId) {
        if (userId == null) {
            return false;
        }
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Lấy số lượng session đang kết nối của người dùng (phục vụ giám sát và kiểm
     * thử).
     */
    public int getSessionCount(UUID userId) {
        if (userId == null) {
            return 0;
        }
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Lấy trạng thái hiện diện chi tiết của một người dùng.
     */
    public UserPresenceResponse getUserPresence(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean online = isUserOnline(userId);
        return UserPresenceResponse.builder()
                .userId(userId)
                .online(online)
                .lastSeenAt(online ? null : user.getLastSeenAt()) // neu dang onl thi null
                .build();
    }

    /**
     * Lấy danh sách trạng thái hiện diện hàng loạt cho nhiều người dùng (ví dụ:
     * danh sách bạn bè, thành viên nhóm).
     * Tự động khử trùng lặp và giới hạn tối đa 100 người dùng mỗi lượt truy vấn để
     * bảo vệ bộ nhớ và database.
     */
    public List<UserPresenceResponse> getUsersPresence(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Khử trùng lặp và chặn giới hạn an toàn tối đa 100 user IDs
        Set<UUID> uniqueIds = userIds.stream()
                .filter(Objects::nonNull)
                .limit(100)
                .collect(java.util.stream.Collectors.toSet());

        if (uniqueIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> users = userRepository.findAllById(uniqueIds);
        List<UserPresenceResponse> result = new ArrayList<>(users.size());

        for (User user : users) {
            boolean online = isUserOnline(user.getUserId());
            result.add(UserPresenceResponse.builder()
                    .userId(user.getUserId())
                    .online(online)
                    .lastSeenAt(online ? null : user.getLastSeenAt())
                    .build());
        }

        return result;
    }

    /**
     * Phát broadcast sự thay đổi trạng thái hiện diện qua WebSocket STOMP.
     * - /topic/users.presence: Topic chung toàn hệ thống
     * - /topic/users.{userId}.presence: Topic riêng cho từng người dùng cụ thể
     */
    public void broadcastPresence(UUID userId, boolean isOnline, LocalDateTime lastSeenAt) {
        UserPresenceResponse payload = UserPresenceResponse.builder()
                .userId(userId)
                .online(isOnline)
                .lastSeenAt(lastSeenAt)
                .build();

        try {
            // Broadcast toàn hệ thống
            messagingTemplate.convertAndSend("/topic/users.presence", payload);
            // Broadcast kênh riêng của user
            messagingTemplate.convertAndSend("/topic/users." + userId + ".presence", payload);
            log.info("Broadcast UserPresence: userId = {}, isOnline = {}, lastSeenAt = {}", userId, isOnline,
                    lastSeenAt);
        } catch (Exception e) {
            log.error("Lỗi khi gửi WebSocket broadcast UserPresence cho user {}: {}", userId, e.getMessage());
        }
    }
}
