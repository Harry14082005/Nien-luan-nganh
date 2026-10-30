package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.SendMessageRequest;
import com.nhom8.nien_luan_nganh.dto.request.UpdateMessageRequest;
import com.nhom8.nien_luan_nganh.dto.response.CursorResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageHistoryResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.MessageMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nhom8.nien_luan_nganh.dto.request.ReactionRequest;
import com.nhom8.nien_luan_nganh.dto.response.ReactionResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service xử lý các nghiệp vụ liên quan đến Tin nhắn trong phòng chat.
 * Sử dụng Constructor Injection (@RequiredArgsConstructor) theo chuẩn Spring
 * Boot.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final FriendshipRepository friendshipRepository;
    private final MessageHistoryRepository messageHistoryRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MessageMapper messageMapper;

    /**
     * Nghiệp vụ 1: Gửi tin nhắn vào phòng chat.
     * Hỗ trợ các định dạng: Text, Image, File và Trả lời tin nhắn (Reply).
     * Kiểm tra bảo mật:
     * - Bắt buộc là thành viên trong phòng chat.
     * - Nếu là phòng chat 1-1 (DIRECT): Kiểm tra trạng thái chặn (Block) giữa 2
     * người dùng.
     * - Kiểm tra tin nhắn cha khi reply (phải cùng phòng và chưa bị thu hồi).
     * - Cập nhật thời gian hoạt động của Room (đẩy phòng chat lên đầu danh bạ).
     *
     * @param currentUserId Khóa chính UUID của người gửi tin nhắn (lấy từ Token)
     * @param roomId        Khóa chính UUID của phòng chat nhận tin nhắn
     * @param request       Dữ liệu tin nhắn gửi lên (content, messageType,
     *                      repliedMessageId)
     * @return MessageResponse Chi tiết tin nhắn sau khi lưu thành công
     */
    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, UUID roomId, SendMessageRequest request) {
        // 1. Kiểm tra người gửi có tồn tại không
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra bảo mật: Người gửi BẮT BUỘC phải là thành viên trong phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Nếu là phòng chat 1-1 (DIRECT): Kiểm tra trạng thái Chặn (Block)
        if (room.getRoomType() == RoomType.DIRECT) {
            List<RoomMember> members = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
            for (RoomMember member : members) {
                UUID otherUserId = member.getRoomMemberId().getUserId();
                if (!otherUserId.equals(currentUserId)) {
                    Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(currentUserId, otherUserId);
                    if (friendship.isPresent() && friendship.get().getFriendshipStatus() == FriendshipStatus.BLOCKED) {
                        throw new AppException(ErrorCode.USER_BLOCKED);
                    }
                }
            }
        }

        // 5. Xác định loại tin nhắn và validate nội dung
        MessageType messageType = (request.getMessageType() != null) ? request.getMessageType() : MessageType.TEXT;
        String content = (request.getContent() != null) ? request.getContent().trim() : null;
        if (messageType == MessageType.TEXT && (content == null || content.isBlank())) {
            throw new AppException(ErrorCode.MESSAGE_CONTENT_REQUIRED);
        }

        // 6. Xử lý tính năng Trả lời tin nhắn (Reply) nếu có repliedMessageId
        Message repliedMessage = null;
        if (request.getRepliedMessageId() != null) {
            repliedMessage = messageRepository.findById(request.getRepliedMessageId())
                    .orElseThrow(() -> new AppException(ErrorCode.REPLIED_MESSAGE_NOT_FOUND));

            // Ràng buộc bảo mật: Tin nhắn được reply phải thuộc CÙNG một phòng chat
            if (!repliedMessage.getRoom().getRoomId().equals(roomId)) {
                throw new AppException(ErrorCode.REPLIED_MESSAGE_NOT_IN_SAME_ROOM);
            }

            // Ràng buộc bảo mật: Không được reply tin nhắn đã bị thu hồi
            if (repliedMessage.getIsRevoked() != null && repliedMessage.getIsRevoked()) {
                throw new AppException(ErrorCode.MESSAGE_REVOKED);
            }
        }

        // 7. Khởi tạo và lưu tin nhắn mới vào cơ sở dữ liệu
        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .messageType(messageType)
                .content(content)
                .repliedMessage(repliedMessage)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        // 8. Cập nhật thời gian hoạt động của phòng chat (đẩy phòng chat lên đầu danh
        // bạ)
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // 9. Chuyển đổi sang MessageResponse và trả về cho Controller
        return messageMapper.toMessageResponse(savedMessage);
    }

    /**
     * Nghiệp vụ 2: Chỉnh sửa nội dung tin nhắn.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải tồn tại và thuộc phòng chat đó.
     * - Chỉ người gửi tin nhắn (Sender) mới có quyền chỉnh sửa.
     * - Không được sửa tin nhắn đã bị thu hồi (isRevoked = true).
     * - Chỉ hỗ trợ sửa tin nhắn dạng TEXT.
     * - Lưu nội dung cũ vào bảng `message_histories` để bảo toàn lịch sử.
     * - Cập nhật nội dung mới và đánh dấu `isEdited = true`.
     *
     * @param currentUserId Khóa chính UUID người dùng thực hiện sửa (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn cần sửa
     * @param request       Nội dung mới của tin nhắn
     * @return MessageResponse Chi tiết tin nhắn sau khi chỉnh sửa
     */
    @Transactional
    public MessageResponse updateMessage(UUID currentUserId, UUID roomId, UUID messageId,
            UpdateMessageRequest request) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn cần chỉnh sửa
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Kiểm tra quyền: Chỉ người gửi tin nhắn mới được phép chỉnh sửa
        if (!message.getSender().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_EDIT_MESSAGE);
        }

        // 7. Kiểm tra trạng thái: Không được sửa tin nhắn đã bị thu hồi
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 8. Chỉ cho phép chỉnh sửa tin nhắn văn bản (TEXT)
        if (message.getMessageType() != MessageType.TEXT) {
            throw new AppException(ErrorCode.CANNOT_EDIT_MESSAGE);
        }

        // Validate nội dung chỉnh sửa không được để trống
        if (request.getContent() == null || request.getContent().trim().isBlank()) {
            throw new AppException(ErrorCode.MESSAGE_CONTENT_REQUIRED);
        }

        String newContent = request.getContent().trim();
        // 9. Nếu nội dung mới khác nội dung hiện tại, tiến hành lưu lịch sử và cập nhật
        if (!newContent.equals(message.getContent())) {
            // Lưu bản chụp nội dung cũ vào bảng lịch sử
            MessageHistory history = MessageHistory.builder()
                    .message(message)
                    .content(message.getContent())
                    .editedAt(LocalDateTime.now())
                    .build();
            messageHistoryRepository.save(history);

            // Cập nhật nội dung mới cho tin nhắn
            message.setContent(newContent);
            message.setIsEdited(true);
            message = messageRepository.save(message);
        }

        return messageMapper.toMessageResponse(message);
    }

    /**
     * Nghiệp vụ 3: Thu hồi tin nhắn (Xóa mềm - Soft Delete).
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải tồn tại và thuộc phòng chat đó.
     * - Tin nhắn chưa bị thu hồi trước đó.
     * - Phân quyền:
     * + Người gửi (Sender) có quyền thu hồi tin nhắn của chính mình.
     * + Trưởng nhóm (OWNER) hoặc Quản trị viên (ADMIN) có quyền thu hồi tin nhắn
     * của thành viên khác để kiểm duyệt.
     * + Thành viên bình thường không được thu hồi tin nhắn của người khác.
     * - Khi thu hồi: Đánh dấu isRevoked = true, revokedAt = now(), và xoá nội dung
     * gốc (content = null) để bảo mật.
     *
     * @param currentUserId Khóa chính UUID người dùng thực hiện thu hồi (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn cần thu hồi
     * @return MessageResponse Chi tiết tin nhắn sau khi thu hồi
     */
    @Transactional
    public MessageResponse revokeMessage(UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn cần thu hồi
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Kiểm tra tin nhắn đã bị thu hồi trước đó chưa
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Kiểm tra quyền: Chỉ người gửi mới có quyền thu hồi tin nhắn của chính mình
        if (!message.getSender().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_REVOKE_MESSAGE);
        }

        // 8. Đánh dấu thu hồi (xoá mềm) và xóa triệt để nội dung trong CSDL
        message.setIsRevoked(true);
        message.setRevokedAt(LocalDateTime.now());
        message.setContent(null); // Gỡ nội dung gốc ra khỏi database để bảo mật thông tin
        message.setIsPinned(false); // Gỡ ghim nếu tin nhắn đang được ghim

        // Xóa sạch toàn bộ lịch sử chỉnh sửa cũ trực tiếp từ repository
        messageHistoryRepository.deleteByMessage_MessageId(messageId);
        if (message.getMessageHistories() != null) {
            message.getMessageHistories().clear();
        }

        // Xóa toàn bộ cảm xúc trên tin nhắn bị thu hồi
        messageReactionRepository.deleteByMessageId(messageId);
        if (message.getMessageReactions() != null) {
            message.getMessageReactions().clear();
        }

        Message savedMessage = messageRepository.save(message);

        return messageMapper.toMessageResponse(savedMessage);
    }

    /**
     * Nghiệp vụ 4: Xem lịch sử chỉnh sửa của một tin nhắn.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải tồn tại và thuộc phòng chat đó.
     * - Ràng buộc bảo mật: Không được xem lịch sử của tin nhắn đã bị thu hồi.
     * - Trả về danh sách các phiên bản nội dung trước đó sắp xếp theo thời gian mới
     * nhất (DESC).
     *
     * @param currentUserId Khóa chính UUID người dùng yêu cầu (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn
     * @return Danh sách các bản ghi lịch sử MessageHistoryResponse
     */
    @Transactional(readOnly = true)
    public List<MessageHistoryResponse> getMessageHistory(UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Ràng buộc bảo mật: Không được xem lịch sử của tin nhắn đã bị thu hồi
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Lấy danh sách lịch sử chỉnh sửa từ repository
        List<MessageHistory> histories = messageHistoryRepository.findHistoryByMessageId(messageId);

        return messageMapper.toMessageHistoryResponseList(histories);
    }

    /**
     * Nghiệp vụ 5: Ghim tin nhắn trong phòng chat.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải tồn tại và thuộc phòng chat đó.
     * - Không được ghim tin nhắn đã bị thu hồi.
     *
     * @param currentUserId Khóa chính UUID người dùng thực hiện ghim (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn cần ghim
     * @return MessageResponse Chi tiết tin nhắn sau khi ghim
     */
    @Transactional
    public MessageResponse pinMessage(UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn cần ghim
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Không được ghim tin nhắn đã bị thu hồi
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Đánh dấu ghim
        message.setIsPinned(true);
        Message savedMessage = messageRepository.save(message);

        return messageMapper.toMessageResponse(savedMessage);
    }

    /**
     * Nghiệp vụ 6: Bỏ ghim tin nhắn trong phòng chat.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải tồn tại và thuộc phòng chat đó.
     *
     * @param currentUserId Khóa chính UUID người dùng thực hiện bỏ ghim (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn cần bỏ ghim
     * @return MessageResponse Chi tiết tin nhắn sau khi bỏ ghim
     */
    @Transactional
    public MessageResponse unpinMessage(UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn cần bỏ ghim
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Đánh dấu bỏ ghim
        message.setIsPinned(false);
        Message savedMessage = messageRepository.save(message);

        return messageMapper.toMessageResponse(savedMessage);
    }

    /**
     * Nghiệp vụ 7: Lấy danh sách tin nhắn đã ghim trong phòng chat.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Phòng chat phải tồn tại.
     * - Trả về danh sách các tin nhắn đang được ghim (isPinned = true) và chưa bị
     * thu hồi.
     *
     * @param currentUserId Khóa chính UUID người dùng yêu cầu (từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @return Danh sách tin nhắn đã ghim (MessageResponse)
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getPinnedMessages(UUID currentUserId, UUID roomId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Lấy danh sách tin nhắn đã ghim từ repository
        List<Message> pinnedMessages = messageRepository.findPinnedMessagesByRoomId(roomId);

        return messageMapper.toMessageResponseList(pinnedMessages);
    }

    /**
     * Nghiệp vụ 8: Thả hoặc cập nhật cảm xúc trên tin nhắn (Reactions).
     * Hỗ trợ các cảm xúc: LIKE, LOVE, HAHA, WOW, SAD, ANGRY.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải thuộc phòng chat đó và không được ở trạng thái đã bị thu hồi.
     * - Nếu người dùng đã thả cảm xúc trước đó: Cập nhật sang loại cảm xúc mới.
     * - Nếu chưa thả cảm xúc: Tạo mới phản ứng cảm xúc.
     *
     * @param currentUserId Khóa chính UUID của người thả cảm xúc (lấy từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn
     * @param request       Dữ liệu loại cảm xúc gửi lên (ReactionRequest)
     * @return ReactionResponse Chi tiết cảm xúc sau khi lưu
     */
    @Transactional
    public com.nhom8.nien_luan_nganh.dto.response.ReactionResponse reactMessage(
            UUID currentUserId, UUID roomId, UUID messageId, ReactionRequest request) {
        // 1. Kiểm tra người dùng có tồn tại không
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn cần thả cảm xúc
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Ràng buộc bảo mật: Không được thả cảm xúc vào tin nhắn đã bị thu hồi
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Kiểm tra loại cảm xúc
        if (request == null || request.getReactionType() == null) {
            throw new AppException(ErrorCode.REACTION_INVALID);
        }

        // 8. Tìm xem người dùng đã thả cảm xúc cho tin nhắn này trước đó chưa
        MessageReactionId reactionId = MessageReactionId.builder()
                .messageId(messageId)
                .userId(currentUserId)
                .build();

        MessageReaction reaction = messageReactionRepository.findById(reactionId)
                .map(existingReaction -> {
                    existingReaction.setReactionType(request.getReactionType().name());
                    return existingReaction;
                })
                .orElseGet(() -> MessageReaction.builder()
                        .messageReactionId(reactionId)
                        .message(message)
                        .user(user)
                        .reactionType(request.getReactionType().name())
                        .createdAt(LocalDateTime.now())
                        .build());

        MessageReaction savedReaction = messageReactionRepository.save(reaction);
        return messageMapper.toReactionResponse(savedReaction);
    }

    /**
     * Nghiệp vụ 9: Gỡ cảm xúc đã thả trên tin nhắn.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải thuộc phòng chat đó.
     * - Xóa cảm xúc của người dùng khỏi tin nhắn.
     *
     * @param currentUserId Khóa chính UUID của người dùng (lấy từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn
     */
    @Transactional
    public void removeReaction(UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Chặn thao tác nếu tin nhắn đã bị thu hồi
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Xóa reaction bằng composite key có sẵn
        MessageReactionId reactionId = MessageReactionId.builder()
                .messageId(messageId)
                .userId(currentUserId)
                .build();

        messageReactionRepository.deleteById(reactionId);
    }

    /**
     * Nghiệp vụ 10: Xem danh sách tất cả các cảm xúc đã thả trên một tin nhắn.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Tin nhắn phải thuộc phòng chat đó và chưa bị thu hồi.
     * - Trả về danh sách chi tiết các thành viên và loại cảm xúc tương ứng.
     *
     * @param currentUserId Khóa chính UUID của người dùng yêu cầu (lấy từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param messageId     Khóa chính UUID của tin nhắn
     * @return Danh sách ReactionResponse
     */
    @Transactional(readOnly = true)
    public List<ReactionResponse> getMessageReactions(
            UUID currentUserId, UUID roomId, UUID messageId) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Tìm tin nhắn
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 5. Kiểm tra tin nhắn có thuộc phòng chat này không
        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 6. Nếu tin nhắn đã bị thu hồi: Không cho phép xem cảm xúc
        if (Boolean.TRUE.equals(message.getIsRevoked())) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }

        // 7. Lấy danh sách cảm xúc từ repository (kèm thông tin user)
        List<MessageReaction> reactions = messageReactionRepository.findByMessageIdWithUser(messageId);
        return messageMapper.toReactionResponseList(reactions);
    }

    /**
     * Nghiệp vụ 11: Lấy danh sách tin nhắn trong phòng chat theo phân trang con trỏ
     * (Cursor-based Pagination).
     * Phục vụ tính năng tải tin nhắn ban đầu và cuộn vô tận (Infinite Scroll) khi
     * xem lịch sử cũ hơn.
     * Quy tắc nghiệp vụ:
     * - Người dùng phải là thành viên trong phòng chat.
     * - Nếu không truyền cursor: Lấy danh sách tin nhắn mới nhất.
     * - Nếu truyền cursor: Lấy danh sách các tin nhắn cũ hơn tin nhắn con trỏ
     * (createdAt < cursor.createdAt).
     * - Giới hạn limit từ 1 đến 100 (mặc định 20).
     *
     * @param currentUserId Khóa chính UUID của người dùng yêu cầu (lấy từ Token)
     * @param roomId        Khóa chính UUID của phòng chat
     * @param cursorId      Khóa chính UUID của tin nhắn làm con trỏ mốc (tùy chọn)
     * @param limit         Số lượng tin nhắn cần lấy
     * @return CursorResponse<MessageResponse> chứa danh sách tin nhắn, nextCursor
     *         và hasMore
     */
    @Transactional(readOnly = true)
    public CursorResponse<MessageResponse> getMessages(UUID currentUserId, UUID roomId, UUID cursorId, int limit) {
        // 1. Kiểm tra người dùng có tồn tại không
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Kiểm tra phòng chat có tồn tại không
        roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 3. Kiểm tra thành viên phòng chat
        boolean isMember = roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId,
                roomId);
        if (!isMember) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 4. Chuẩn hóa giới hạn số lượng tin nhắn (1 <= limit <= 100)
        int effectiveLimit = (limit <= 0) ? 20 : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        Page<Message> messagePage;
        if (cursorId != null) {
            // Kiểm tra tin nhắn con trỏ có tồn tại không
            messageRepository.findById(cursorId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

            messagePage = messageRepository.findByRoomIdBefore(roomId, cursorId, pageable);
        } else {
            messagePage = messageRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId, pageable);
        }

        // Xử lý dữ liệu trả về cho Frontend (Xac dinh nextCursor & hasMore)
        List<MessageResponse> messageResponses = messageMapper.toMessageResponseList(messagePage.getContent());

        // Xác định nextCursor: ID của tin nhắn cũ nhất trong batch hiện tại (phần tử
        // cuối danh sách)
        UUID nextCursor = (messageResponses.isEmpty() || !messagePage.hasNext())
                ? null
                : messageResponses.get(messageResponses.size() - 1).getMessageId();

        return CursorResponse.<MessageResponse>builder()
                .data(messageResponses)
                .nextCursor(nextCursor)
                .hasMore(messagePage.hasNext())
                .build();
    }

    // cập nhật tin nhắn mới nhất của phòng (hiển thị ngoài danh sách phòng)

//    public void updateLastMessage(Room room, Message message, String preview) {
//        room.setLastMessageId(message.getMessageId());
//        room.setLastMessageContent(preview);
//        room.setLastMessageSenderId(message.getSender().getUserId());
//        room.setLastMessageAt(message.getCreatedAt());
//    }
}
