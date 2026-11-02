package com.nhom8.nien_luan_nganh.listener;

import com.nhom8.nien_luan_nganh.config.UserDetailsImpl;
import com.nhom8.nien_luan_nganh.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;

/**
 * Listener theo dõi các sự kiện kết nối và ngắt kết nối WebSocket của Spring STOMP.
 * Tự động chuyển giao cho UserPresenceService xử lý đếm session và cập nhật trạng thái Online/Offline.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserPresenceService userPresenceService;

    /**
     * Bắt sự kiện khi Client kết nối thành công vào WebSocket Message Broker.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // 1. Ưu tiên lấy trực tiếp từ Event
        Principal principal = event.getUser();
        String sessionId = null;

        // 2. Nếu thiếu thông tin, bóc tách sâu hơn qua StompHeaderAccessor
        if (event.getMessage() != null) {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            sessionId = headerAccessor.getSessionId();
            if (principal == null) {
                principal = headerAccessor.getUser();
            }
        }

        UUID userId = extractUserId(principal);

        if (userId != null && sessionId != null) {
            log.info("WebSocket SessionConnectedEvent: userId = {}, sessionId = {}", userId, sessionId);
            userPresenceService.addSession(userId, sessionId);
        } else {
            log.warn("WebSocket SessionConnectedEvent thiếu thông tin: userId = {}, sessionId = {}", userId, sessionId);
        }
    }

    /**
     * Bắt sự kiện khi Client ngắt kết nối WebSocket (do đóng tab, mạng rớt, hoặc gửi DISCONNECT).
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // SessionDisconnectEvent luôn cung cấp trực tiếp sessionId và User
        String sessionId = event.getSessionId();
        Principal principal = event.getUser();

        // Fallback kiểm tra trong header nếu event.getUser() bị rỗng (thường gặp khi socket đứt đột ngột)
        if (principal == null && event.getMessage() != null) {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            principal = headerAccessor.getUser();
        }

        UUID fallbackUserId = extractUserId(principal);
        log.info("WebSocket SessionDisconnectEvent: sessionId = {}, fallbackUserId = {}", sessionId, fallbackUserId);

        userPresenceService.removeSession(sessionId, fallbackUserId);
    }

    /**
     * Hàm tiện ích trích xuất UUID người dùng từ đối tượng Principal của Spring Security.
     * Hỗ trợ đa dạng các kiểu lưu trữ Principal trong hệ thống.
     */
    private UUID extractUserId(Principal principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof Authentication authentication) {
            Object principalObj = authentication.getPrincipal();

            // Trường hợp 1: Lưu UserDetailsImpl
            if (principalObj instanceof UserDetailsImpl userDetails) {
                return userDetails.getUserId();
            }

            // Trường hợp 2: Principal trực tiếp là UUID
            if (principalObj instanceof UUID directUuid) {
                return directUuid;
            }

            // Trường hợp 3: Principal là chuỗi UUID
            if (principalObj instanceof String uuidStr) {
                try {
                    return UUID.fromString(uuidStr);
                } catch (IllegalArgumentException ignored) {
                    // Không phải định dạng UUID, chuyển tiếp sang cách duyệt getName()
                }
            }
        }

        // Trường hợp 4: Lấy từ principal.getName() chuẩn
        try {
            return UUID.fromString(principal.getName());
        } catch (Exception ignored) {
            return null;
        }
    }
}
