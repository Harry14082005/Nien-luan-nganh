package com.nhom8.nien_luan_nganh.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.messaging.MessageDeliveryException;
import java.util.Map;
import java.util.UUID;

/**
 * Cấu hình WebSocket STOMP Message Broker cho hệ thống Chat Real-time.
 * Tích hợp xác thực danh tính người dùng qua JWT Token ở cả 2 bước:
 * 1. HTTP Handshake (/ws?token=...)
 * 2. STOMP CONNECT frame (Authorization: Bearer ...)
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Đăng ký endpoint kết nối WebSocket Handshake (/ws).
     * Cho phép kết nối từ Vue.js Frontend (CORS pattern "*").
     * Hỗ trợ cả SockJS fallback (/ws) và Native WebSocket thuần (/ws-raw).
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint chính hỗ trợ SockJS (dành cho thư viện SockJS client trên browser)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor())
                // Danh cho web browser. Neu mang user bi chan Websocket (tuong lua cong ty,
                // proxy cu)
                // SockJS auto chuyen sang co che gia lap nhu HTTP Streaming hoac Long-Polling
                // de khong dut mang
                .withSockJS();

        // Endpoint WebSocket thuần riêng biệt (Postman, Mobile App, Native WS client)
        // Đặt /ws-raw để tránh xung đột HandlerMapping với endpoint /ws của SockJS
        // Danh cho Postman hoac App dt khong dung SockJS
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new JwtHandshakeInterceptor());
    }

    /**
     * Cấu hình Message Broker định tuyến tin nhắn:
     * - /topic: Broadcast tin nhắn công cộng / phòng chat nhóm (ví dụ
     * /topic/room.{roomId})
     * - /queue: Gửi tin nhắn riêng / thông báo cá nhân (ví dụ
     * /user/queue/notifications)
     * - /app: Tiền tố các event do Client gửi lên Server, Spring chuyen tin nhan
     * vao Controller co gang @MessageMapping
     * - /user: Tiền tố định tuyến tin nhắn riêng cho từng User cụ thể
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Cấu hình Interceptor chặn kênh Inbound để xác thực JWT Token khi nhận STOMP
     * CONNECT frame.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            // chanel: connect, disconnect, send data
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                // Bat ngay luc Client gui CONNECT
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = null;

                    // 1. Lấy token từ Header STOMP (Authorization: Bearer <token>)
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    } else if (StringUtils.hasText(accessor.getFirstNativeHeader("token"))) {
                        token = accessor.getFirstNativeHeader("token");
                    }

                    // 2. Nếu header không có, lấy token đã lưu trong Session Attributes từ bước
                    // Handshake (?token=...)
                    if (!StringUtils.hasText(token) && accessor.getSessionAttributes() != null) {
                        token = (String) accessor.getSessionAttributes().get("jwt_token");
                    }

                    // 3. Tiến hành giải mã và xác thực Token
                    if (StringUtils.hasText(token)) {
                        try {
                            if (!jwtService.isTokenExpired(token)) {
                                UUID userId = jwtService.extractUserId(token);
                                if (userId != null) {
                                    // loadUserById(), co token roi van can vi: (JWT la statless) Truong hop user
                                    // vua bi xoa tai khoan hay bi admin khoa 1 phut truoc, nhung token van con, he
                                    // thong van phai truy xuat db de xem trang thai (isAccountNonLocked,
                                    // isEnabled,..)
                                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                                    if (jwtService.isTokenValid(token, userDetails)) {
                                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities());

                                        // Gán Principal vào phiên WebSocket STOMP
                                        // O bat ki dau trong controller, khi server dinh tuyen tinh nhan,... Spring deu
                                        // biet chinh xac no thuoc ve ai
                                        accessor.setUser(authentication);
                                        log.info("WebSocket kết nối thành công: userId = {}", userId);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Xác thực JWT WebSocket thất bại: {}", e.getMessage());
                        }
                    }

                    // 4. BẢO MẬT: Bắt buộc phải xác thực thành công, nếu không ngắt kết nối ngay
                    // lập tức!
                    if (accessor.getUser() == null) {
                        log.warn("Từ chối kết nối WebSocket: Chưa xác thực danh tính hoặc Token không hợp lệ");
                        throw new MessageDeliveryException("Unauthorized: Token không hợp lệ hoặc thiếu");
                    }
                }
                return message;
            }
        });
    }

    /**
     * Interceptor xử lý giai đoạn HTTP Handshake:
     * Trích xuất tham số ?token=... từ URL và cất vào Session Attributes để STOMP
     * CONNECT frame tái sử dụng.
     */
    private static class JwtHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String token = servletRequest.getServletRequest().getParameter("token");
                if (StringUtils.hasText(token)) {
                    attributes.put("jwt_token", token);
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
            // Không cần xử lý sau bắt tay
        }
    }
}
