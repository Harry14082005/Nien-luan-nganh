package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddPollOptionRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreatePollRequest;
import com.nhom8.nien_luan_nganh.dto.request.VotePollRequest;
import com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent;
import com.nhom8.nien_luan_nganh.dto.response.*;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.*;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.PollMapper;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PollService {
    private static final int MAX_OPTIONS = 10;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PollRepository pollRepository;
    private final PollResourceRepository pollResourceRepository;
    private final MessageRepository messageRepository;
    private final PollMapper pollMapper;
    private final UserMapper userMapper;
    private final PollVoteRepository pollVoteRepository;
    private final RoomService roomService;
    private final com.nhom8.nien_luan_nganh.mapper.MessageMapper messageMapper;

    private final ApplicationEventPublisher eventPublisher;

    // Tạo poll
    @Transactional
    public PollResponse createPoll(UUID roomId, CreatePollRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        // nếu là phòng DIRECT thì không có poll
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        // kiểm tra đầu vào
        List<String> texts = normalizeOptions(request.getOptions());
        if (texts.size() < 2) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if (texts.size() > MAX_OPTIONS) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // tạo poll
        Poll poll = Poll.builder()
                .room(room)
                .createdBy(actor.getUser())
                .question(request.getQuestion().trim())
                .build();
        // thêm lựa chọn vô
        for (String text : texts) {
            PollOption option = PollOption.builder()
                    .poll(poll)
                    .optionText(text)
                    .build();
            poll.getOptions().add(option);
        }

        pollRepository.save(poll);

        // tạo thành dạng tin nhắn
        Message message = Message.builder()
                .room(room)
                .sender(actor.getUser())
                .messageType(MessageType.POLL)
                .content(poll.getQuestion())
                .build();
        message = messageRepository.save(message);

        poll.setMessage(message);
        pollRepository.flush();

        // Lưu liên kết PollResource
        PollResource pollResource = PollResource.builder()
                .id(new PollResourceId(message.getMessageId(), poll.getPollId()))
                .message(message)
                .poll(poll)
                .build();
        pollResourceRepository.save(pollResource);

        /// xử lý Websocket

        /// todo: sẽ xử lý trong roomservice
        roomService.updateLastActivity(
                room,
                message.getMessageId(),
                "Bình chọn: " + preview(poll.getQuestion()),
                actor.getUser(),
                message.getCreatedAt());

        // broadcast tin nhắn POLL
        MessageResponse messageResponse = buildPollMessageResponse(message, poll.getPollId());
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));

        // xử lý cập nhật tin nhắn mới nhất
        // broadcast PollEvent để client vẽ giao diện poll (dùng null cho currentUserId
        // để không bị rò rỉ cờ votedByMe của một user cụ thể sang cả phòng)
        PollResponse broadcastPoll = toResponse(poll, null);
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".poll",
                PollEventResponse.builder()
                        .type(PollEventType.POLL_CREATED)
                        .roomId(roomId)
                        .poll(broadcastPoll)
                        .build()));

        // Thông báo hệ thống: ghi nhận sự kiện tạo poll mới
        createAndBroadcastSystemMessage(room, actor.getUser(),
                actor.getUser().getUsername() + " đã tạo một cuộc bình chọn");

        return toResponse(poll, currentUserId);
    }

    // Thêm lựa chọn sau khi tạo
    @Transactional
    public PollResponse addOption(UUID roomId, UUID pollId, AddPollOptionRequest request, UUID currentUserId) {
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        Poll poll = getPollInRoomOrThrow(roomId, pollId);
        ensureActive(poll);

        String text = request.getOptionText().trim();
        if (text.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        // nếu lượt đã tối đa thì hông thêm được
        if (poll.getOptions().size() >= MAX_OPTIONS) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // kiểm tra không trùng với lựa chọn đã có
        for (PollOption existing : poll.getOptions()) {
            if (existing.getOptionText().equalsIgnoreCase(text)) {
                throw new AppException(ErrorCode.EXISTING_POLL_OPTION);
            }
        }

        PollOption option = PollOption.builder()
                .poll(poll)
                .optionText(text)
                .build();
        poll.getOptions().add(option);
        pollRepository.flush(); // để option mới có id trước khi dựng response

        // TODO: Websocket (broadcast PollResponse không gắn votedByMe của riêng user
        // nào)
        PollResponse broadcastPoll = toResponse(poll, null);
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".poll",
                PollEventResponse.builder()
                        .type(PollEventType.POLL_OPTION_ADDED)
                        .roomId(roomId)
                        .poll(broadcastPoll)
                        .build()));

        // Đưa thẻ bình chọn lên vị trí mới nhất
        bumpPollMessageToBottom(poll, actor.getUser());

        // Thông báo hệ thống: ghi nhận sự kiện thêm phương án mới
        createAndBroadcastSystemMessage(poll.getRoom(), actor.getUser(),
                actor.getUser().getUsername() + " đã thêm phương án \"" + preview(text) + "\" vào cuộc bình chọn");

        return toResponse(poll, currentUserId);
    }

    // Bỏ phiếu / đổi phiếu
    // Client gửi toàn bộ danh sách option đang chọn. Danh sách rỗng = rút phiếu.
    @Transactional
    public PollResponse vote(UUID roomId, UUID pollId, VotePollRequest request, UUID currentUserId) {
        RoomMember member = getActiveMemberOrThrow(roomId, currentUserId);

        Poll poll = getPollInRoomOrThrow(roomId, pollId);
        ensureActive(poll); // poll đóng rồi thì không thay đổi được

        // gom tất cả pollOption của poll lại
        Map<UUID, PollOption> optionMap = new HashMap<>();
        for (PollOption option : poll.getOptions()) {
            optionMap.put(option.getPollOptionId(), option);
        }

        // các option mà user muốn chọn
        Set<UUID> wanted = new HashSet<>(request.getOptionIds());

        // kiểm tra có option nào không khớp id hay không
        for (UUID optionId : wanted) {
            if (!optionMap.containsKey(optionId)) {
                throw new AppException(ErrorCode.POLL_OPTION_NOT_FOUND);
            }
        }

        // lấy trạng thái vote hiện tại của user trong poll
        List<PollVote> currentVotes = pollVoteRepository.findByPollOption_Poll_PollIdAndUser_UserId(pollId,
                currentUserId);
        Set<UUID> currentOptionIds = new HashSet<>();

        // lọc ra những lựa chọn đã bị bỏ chọn
        List<PollVote> votesToRemove = new ArrayList<>();
        for (PollVote vote : currentVotes) {
            UUID optionId = vote.getPollOption().getPollOptionId();
            currentOptionIds.add(optionId);

            if (!wanted.contains(optionId)) {
                votesToRemove.add(vote);
            }
        }

        // lọc ra những lựa chọn mà trước đó chưa có
        List<PollVote> votesToAdd = new ArrayList<>();
        for (UUID optionId : wanted) {
            if (!currentOptionIds.contains(optionId)) {
                PollVote newVote = PollVote.builder()
                        .pollVoteId(new PollVoteId(optionId, currentUserId))
                        .pollOption(optionMap.get(optionId))
                        .user(member.getUser())
                        .build();
                votesToAdd.add(newVote);
            }
        }

        pollVoteRepository.deleteAll(votesToRemove);
        pollVoteRepository.saveAll(votesToAdd);
        pollVoteRepository.flush(); // để query đếm phiếu bên dưới thấy dữ liệu mới

        // TODO WebSocket: đẩy kết quả mới cho cả phòng (broadcast không gắn votedByMe của riêng user nào)
        PollResponse broadcastPoll = toResponse(poll, null);
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".poll",
                PollEventResponse.builder()
                        .type(PollEventType.POLL_VOTED)
                        .roomId(roomId)
                        .poll(broadcastPoll)
                        .build()));

        // Đưa thẻ bình chọn lên vị trí mới nhất (bump) khi có thay đổi vote
        if (!votesToAdd.isEmpty() || !votesToRemove.isEmpty()) {
            bumpPollMessageToBottom(poll, member.getUser());

            // Thông báo hệ thống: ghi nhận sự kiện bình chọn (tạo sau bump để làm
            // lastMessage cho phòng)
            createAndBroadcastSystemMessage(poll.getRoom(), member.getUser(),
                    member.getUser().getUsername() + " đã thực hiện bình chọn");
        }

        return toResponse(poll, currentUserId);
    }

    // Đóng poll: người tạo hoặc admin
    @Transactional
    public PollResponse closePoll(UUID roomId, UUID pollId, UUID currentUserId) {
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);
        Poll poll = getPollInRoomOrThrow(roomId, pollId);

        boolean isCreator = poll.getCreatedBy().getUserId().equals(currentUserId);
        boolean isAdmin = actor.getRoomMemberRole() == RoomRole.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ensureActive(poll); // đã đóng rồi thì báo lỗi
        poll.setIsActive(false);

        // TODO WebSocket: đẩy trạng thái đã đóng
        // cập nhật lastActivity
        if (poll.getMessage() != null) {
            roomService.updateLastActivity(
                    poll.getRoom(),
                    poll.getMessage().getMessageId(),
                    "Bình chọn đã kết thúc: " + preview(poll.getQuestion()),
                    actor.getUser(),
                    LocalDateTime.now());
        }

        PollResponse broadcastPoll = toResponse(poll, null);
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".poll",
                PollEventResponse.builder()
                        .type(PollEventType.POLL_CLOSED)
                        .roomId(roomId)
                        .poll(broadcastPoll)
                        .build()));

        // Đưa thẻ bình chọn lên vị trí mới nhất
        bumpPollMessageToBottom(poll, actor.getUser());

        // Thông báo hệ thống: ghi nhận sự kiện khóa poll
        createAndBroadcastSystemMessage(poll.getRoom(), actor.getUser(),
                actor.getUser().getUsername() + " đã khóa cuộc bình chọn");

        return toResponse(poll, currentUserId);
    }

    // Xem
    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID roomId, UUID pollId, UUID currentUserId) {
        getActiveMemberOrThrow(roomId, currentUserId);
        Poll poll = getPollInRoomOrThrow(roomId, pollId);

        return toResponse(poll, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getPolls(UUID roomId, UUID currentUserId) {
        getActiveMemberOrThrow(roomId, currentUserId);

        List<Poll> polls = pollRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId);

        List<PollResponse> responses = new ArrayList<>();
        for (Poll poll : polls) {
            responses.add(toResponse(poll, currentUserId));
        }
        return responses;
    }

    /// các hàm phụ
    private Room getRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomMember getRoomMemberOrThrow(UUID roomId, UUID userId) {
        return roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
    }

    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = getRoomMemberOrThrow(roomId, userId);
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    // trim, bỏ chuỗi rỗng, bỏ trùng (không phân biệt hoa thường)
    private List<String> normalizeOptions(List<String> raw) {
        Map<String, String> unique = new LinkedHashMap<>();

        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String text = item.trim();
            if (text.isEmpty()) {
                continue;
            }
            String key = text.toLowerCase();
            if (!unique.containsKey(key)) {
                unique.put(key, text);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Poll getPollInRoomOrThrow(UUID roomId, UUID pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new AppException(ErrorCode.POLL_NOT_FOUND));

        if (!poll.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.POLL_NOT_FOUND);
        }
        return poll;
    }

    private PollResponse toResponse(Poll poll, UUID currentUserId) {
        UUID pollId = poll.getPollId();

        PollResponse response = pollMapper.toPollResponse(poll);

        // số phiếu của từng option (1 query)
        Map<UUID, Long> voteCounts = new HashMap<>();
        List<Object[]> rows = pollVoteRepository.countVotesByOption(pollId);
        for (Object[] row : rows) {
            UUID optionId = (UUID) row[0];
            Long count = (Long) row[1];
            voteCounts.put(optionId, count);
        }

        // các option tôi đã chọn
        Set<UUID> myOptionIds = new HashSet<>();
        List<PollVote> myVotes = pollVoteRepository
                .findByPollOption_Poll_PollIdAndUser_UserId(pollId, currentUserId);
        for (PollVote vote : myVotes) {
            myOptionIds.add(vote.getPollOption().getPollOptionId());
        }

        // gom danh sách người vote theo từng option
        Map<UUID, List<PollVoterResponse>> votersByOption = new HashMap<>();
        List<PollVote> allVotes = pollVoteRepository.findAllWithUserByPollId(pollId);
        for (PollVote vote : allVotes) {
            UUID optId = vote.getPollOption().getPollOptionId();
            User u = vote.getUser();
            PollVoterResponse voterDTO = PollVoterResponse.builder()
                    .userId(u.getUserId())
                    .username(u.getUsername())
                    .avatarUrl(u.getAvatarUrl())
                    .votedAt(vote.getVotedAt())
                    .build();
            votersByOption.computeIfAbsent(optId, k -> new ArrayList<>()).add(voterDTO);
        }

        // điền vào từng option của response
        for (PollOptionResponse optionResponse : response.getOptions()) {
            UUID optionId = optionResponse.getPollOptionId();

            Long count = voteCounts.get(optionId);
            if (count == null) {
                optionResponse.setVoteCount(0);
            } else {
                optionResponse.setVoteCount(count);
            }

            optionResponse.setVotedByMe(myOptionIds.contains(optionId));
            optionResponse.setVoters(votersByOption.getOrDefault(optionId, new ArrayList<>()));
        }

        response.setTotalVoters(pollVoteRepository.countVoters(pollId));
        return response;
    }

    // đảm bảo poll chưa bị đóng
    private void ensureActive(Poll poll) {
        if (!Boolean.TRUE.equals(poll.getIsActive())) {
            throw new AppException(ErrorCode.POLL_CLOSED);
        }
    }

    // Tạo tin nhắn POLL mới để đưa cuộc bình chọn lên vị trí mới nhất trong phòng
    // chat (bump)
    private void bumpPollMessageToBottom(Poll poll, User actor) {
        Message bumpMessage = Message.builder()
                .room(poll.getRoom())
                .sender(actor)
                .messageType(MessageType.POLL)
                .content(poll.getQuestion())
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedBumpMessage = messageRepository.save(bumpMessage);

        poll.setMessage(savedBumpMessage);
        pollRepository.save(poll);

        // Lưu liên kết PollResource cho tin nhắn bump mới
        PollResource bumpResource = PollResource.builder()
                .id(new PollResourceId(savedBumpMessage.getMessageId(), poll.getPollId()))
                .message(savedBumpMessage)
                .poll(poll)
                .build();
        pollResourceRepository.save(bumpResource);

        // Cập nhật lastMessage của phòng
        Room room = poll.getRoom();
        LocalDateTime now = savedBumpMessage.getCreatedAt();
        room.setLastMessageId(savedBumpMessage.getMessageId());
        room.setLastMessageContent(poll.getQuestion());
        room.setLastMessageSenderId(actor.getUserId());
        room.setLastMessageSenderName(actor.getUsername());
        room.setLastMessageAt(now);
        room.setUpdatedAt(now);
        roomRepository.save(room);

        // Phát sóng tin nhắn POLL mới qua WebSocket cho tất cả thành viên trong phòng
        MessageResponse messageResponse = buildPollMessageResponse(savedBumpMessage, poll.getPollId());
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + poll.getRoom().getRoomId(), messageResponse));
    }

    // dựng MessageResponse cho tin nhắn POLL để broadcast vào dòng chat
    // giúp frontend hiện poll card trong dòng chat ngay, không cần load lại
    private MessageResponse buildPollMessageResponse(Message message, UUID pollId) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoom().getRoomId())
                .messageType(MessageType.POLL)
                .content(message.getContent())
                .pollId(pollId)
                .createdAt(message.getCreatedAt())
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .sender(userMapper.toUserResponse(message.getSender()))
                .build();
    }

    // Tạo và phát sóng tin nhắn hệ thống (SYSTEM) cho các sự kiện bình chọn
    private void createAndBroadcastSystemMessage(Room room, User actorUser, String systemContent) {
        UUID roomId = room.getRoomId();
        UUID currentUserId = actorUser.getUserId();

        Message systemMessage = Message.builder()
                .room(room)
                .sender(actorUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        LocalDateTime now = savedSystemMessage.getCreatedAt();
        room.setLastMessageId(savedSystemMessage.getMessageId());
        room.setLastMessageContent(systemContent);
        room.setLastMessageSenderId(currentUserId);
        room.setLastMessageSenderName(actorUser.getUsername());
        room.setLastMessageAt(now);
        room.setUpdatedAt(now);
        roomRepository.save(room);

        List<RoomMember> allMembers = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        for (RoomMember m : allMembers) {
            if (!m.getRoomMemberId().getUserId().equals(currentUserId)) {
                m.setLastReadMessageId(null);
            }
        }
        roomMemberRepository.saveAll(allMembers);

        MessageResponse messageResponse = messageMapper.toMessageResponse(savedSystemMessage);
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));
    }

    // hiển thị .. nếu quá dài
    private String preview(String text) {
        if (text.length() > 100) {
            return text.substring(0, 100) + "...";
        }
        return text;
    }

}
