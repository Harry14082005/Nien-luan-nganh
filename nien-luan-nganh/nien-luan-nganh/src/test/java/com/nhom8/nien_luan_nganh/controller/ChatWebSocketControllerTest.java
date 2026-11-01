package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.ChatMessagePayload;
import com.nhom8.nien_luan_nganh.dto.request.SendMessageRequest;
import com.nhom8.nien_luan_nganh.dto.request.TypingPayload;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.dto.response.TypingEventResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private UUID userId;
    private UUID roomId;
    private Principal principal;
    private MessageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        principal = mock(Principal.class);
        lenient().when(principal.getName()).thenReturn(userId.toString());

        UserResponse sender = UserResponse.builder()
                .userId(userId)
                .username("harry_potter")
                .avatarUrl("avatar.png")
                .build();

        sampleResponse = MessageResponse.builder()
                .messageId(UUID.randomUUID())
                .roomId(roomId)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content("Xin chào phòng chat qua WebSocket!")
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Gửi tin nhắn chuẩn duy nhất qua /app/chat.sendMessage thành công và broadcast về /topic/room.{roomId}")
    void sendMessage_Success() {
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .roomId(roomId)
                .content("Nội dung chat chuẩn duy nhất")
                .messageType(MessageType.TEXT)
                .build();

        when(messageService.sendMessage(eq(userId), eq(roomId), any(SendMessageRequest.class)))
                .thenReturn(sampleResponse);

        chatWebSocketController.sendMessage(payload, principal);

        verify(messageService, times(1)).sendMessage(eq(userId), eq(roomId), any(SendMessageRequest.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/room." + roomId), eq(sampleResponse));
    }

    @Test
    @DisplayName("Từ chối gửi tin nhắn khi Principal là null (Chưa xác thực JWT)")
    void sendMessage_Unauthenticated_Ignore() {
        ChatMessagePayload payload = ChatMessagePayload.builder()
                .roomId(roomId)
                .content("Tin nhắn hacker")
                .messageType(MessageType.TEXT)
                .build();

        chatWebSocketController.sendMessage(payload, null);

        verify(messageService, never()).sendMessage(any(), any(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Gửi trạng thái đang gõ chữ (/app/chat.typing) thành công qua MessageService và broadcast về /topic/room.{roomId}.typing")
    void sendTypingStatus_Success() {
        TypingPayload payload = TypingPayload.builder()
                .roomId(roomId)
                .typing(true)
                .build();

        TypingEventResponse eventResponse = TypingEventResponse.builder()
                .roomId(roomId)
                .userId(userId)
                .username("harry_potter")
                .avatarUrl("avatar.png")
                .typing(true)
                .build();

        when(messageService.buildTypingEvent(eq(userId), eq(payload), any(), any()))
                .thenReturn(eventResponse);

        chatWebSocketController.sendTypingStatus(payload, principal);

        verify(messageService, times(1)).buildTypingEvent(eq(userId), eq(payload), any(), any());
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/room." + roomId + ".typing"), eq(eventResponse));
    }

    @Test
    @DisplayName("Chặn gửi trạng thái đang gõ chữ nếu người dùng không phải thành viên phòng chat")
    void sendTypingStatus_NotRoomMember_ThrowsAppException() {
        TypingPayload payload = TypingPayload.builder()
                .roomId(roomId)
                .typing(true)
                .build();

        when(messageService.buildTypingEvent(eq(userId), eq(payload), any(), any()))
                .thenThrow(new AppException(ErrorCode.NOT_ROOM_MEMBER));

        AppException ex = assertThrows(AppException.class,
                () -> chatWebSocketController.sendTypingStatus(payload, principal));
        assertEquals(ErrorCode.NOT_ROOM_MEMBER, ex.getErrorCode());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("Bắt lỗi Validation (@Valid) và gửi thông báo lỗi về /user/queue/errors")
    void handleValidationException_SendsValidationErrorToUserQueue() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("payload", "content", "MESSAGE_CONTENT_REQUIRED");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        chatWebSocketController.handleValidationException(ex, principal);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/errors"),
                any(ApiResponse.class)
        );
    }

    @Test
    @DisplayName("Bắt lỗi nghiệp vụ AppException và gửi thông báo lỗi về /user/queue/errors của người gửi")
    void handleAppException_SendsErrorToUserQueue() {
        AppException ex = new AppException(ErrorCode.NOT_ROOM_MEMBER);

        chatWebSocketController.handleAppException(ex, principal);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/errors"),
                any(ApiResponse.class)
        );
    }

    @Test
    @DisplayName("Bắt lỗi ngoại lệ chung Exception và gửi lỗi UNCATEGORIZED_EXCEPTION về /user/queue/errors")
    void handleGeneralException_SendsGenericErrorToUserQueue() {
        Exception ex = new RuntimeException("Cơ sở dữ liệu mất kết nối");

        chatWebSocketController.handleGeneralException(ex, principal);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/errors"),
                any(ApiResponse.class)
        );
    }
}
