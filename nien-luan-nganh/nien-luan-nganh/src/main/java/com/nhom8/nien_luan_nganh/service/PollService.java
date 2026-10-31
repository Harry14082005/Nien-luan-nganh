package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddPollOptionRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreatePollRequest;
import com.nhom8.nien_luan_nganh.dto.request.VotePollRequest;
import com.nhom8.nien_luan_nganh.dto.response.PollOptionResponse;
import com.nhom8.nien_luan_nganh.dto.response.PollResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.PollMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PollService {
    private static final int MAX_OPTIONS = 10;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PollRepository pollRepository;
    private final MessageRepository messageRepository;
    private final PollMapper pollMapper;
    private final PollVoteRepository pollVoteRepository;
    //Tạo poll
    @Transactional
    public PollResponse createPoll(UUID roomId, CreatePollRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        //nếu là phòng DIRECT thì không có poll
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        //kiểm tra đầu vào
        List<String> texts = normalizeOptions(request.getOptions());
        if(texts.size() < 2){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if(texts.size() > MAX_OPTIONS){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        //tạo poll
        Poll poll = Poll.builder()
                .room(room)
                .createdBy(actor.getUser())
                .question(request.getQuestion().trim())
                .build();
        //thêm lựa chọn vô
        for (String text : texts){
            PollOption option = PollOption.builder()
                    .poll(poll)
                    .optionText(text)
                    .build();
            poll.getOptions().add(option);
        }

        pollRepository.save(poll);

        //tạo thành dạng tin nhắn
        Message message = Message.builder()
                .room(room)
                .sender(actor.getUser())
                .messageType(MessageType.POLL)
                .content(poll.getQuestion())
                .build();
        messageRepository.save(message);

        poll.setMessage(message);

        ///xử lý Websocket
        ////

        //xử lý cập nhật tin nhắn mới nhất

        return toResponse(poll, currentUserId);
    }

    // Thêm lựa chọn sau khi tạo
    @Transactional
    public PollResponse addOption(UUID roomId, UUID pollId, AddPollOptionRequest request, UUID currentUserId) {
        getActiveMemberOrThrow(roomId, currentUserId);

        Poll poll = getPollInRoomOrThrow(roomId, pollId);
        ensureActive(poll);

        String text = request.getOptionText().trim();
        if(text.isEmpty()){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        //nếu lượt đã tối đa thì hông thêm được
        if(poll.getOptions().size() >= MAX_OPTIONS){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        //kiểm tra không trùng với lựa chọn đã có
        for(PollOption existing : poll.getOptions()){
            if(existing.getOptionText().equalsIgnoreCase(text)){
                throw new AppException(ErrorCode.EXISTING_POLL_OPTION);
            }
        }

        PollOption option = PollOption.builder()
                .poll(poll)
                .optionText(text)
                .build();
        poll.getOptions().add(option);
        pollRepository.flush(); // để option mới có id trước khi dựng response

        //TODO: Websocket

        return toResponse(poll, currentUserId);
    }

    // Bỏ phiếu / đổi phiếu
    // Client gửi toàn bộ danh sách option đang chọn. Danh sách rỗng = rút phiếu.
    @Transactional
    public PollResponse vote(UUID roomId, UUID pollId, VotePollRequest request, UUID currentUserId) {
        RoomMember member = getRoomMemberOrThrow(roomId, currentUserId);

        Poll poll = getPollInRoomOrThrow(roomId, pollId);
        ensureActive(poll); //poll đóng  rồi thì không thay đổi được

        // gom tất cả pollOption của poll lại
        Map<UUID, PollOption> optionMap = new HashMap<>();
        for(PollOption option : poll.getOptions()){
            optionMap.put(option.getPollOptionId(), option);
        }

        //các option mà user muốn chọn
        Set<UUID> wanted = new HashSet<>(request.getOptionIds());

        //kiểm tra có option nào không khớp id hay không
        for(UUID optionId : wanted){
            if(!optionMap.containsKey(optionId)){
                throw new AppException(ErrorCode.POLL_OPTION_NOT_FOUND);
            }
        }

        //lấy trạng thái vote hiện tại của user trong poll
        List<PollVote> currentVotes = pollVoteRepository.findByPollOption_Poll_PollIdAndUser_UserId(pollId, currentUserId);
        Set<UUID> currentOptionIds = new HashSet<>();

        //lọc ra những lựa chọn đã bị bỏ chọn
        List<PollVote> votesToRemove = new ArrayList<>();
        for(PollVote vote : currentVotes){
            UUID optionId = vote.getPollOption().getPollOptionId();
            currentOptionIds.add(optionId);

            if (!wanted.contains(optionId)) {
                votesToRemove.add(vote);
            }
        }

        //lọc ra những lựa chọn mà trước đó chưa có
        List<PollVote> votesToAdd = new ArrayList<>();
        for(UUID optionId : wanted){
            if(!currentOptionIds.contains(optionId)){
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
        pollVoteRepository.flush();   // để query đếm phiếu bên dưới thấy dữ liệu mới

        // TODO WebSocket: đẩy kết quả mới cho cả phòng

        return toResponse(poll, currentUserId);
    }

    // Đóng poll: người tạo hoặc admin
    @Transactional
    public PollResponse closePoll(UUID roomId, UUID pollId, UUID currentUserId){
        RoomMember actor = getRoomMemberOrThrow(roomId, currentUserId);
        Poll poll = getPollInRoomOrThrow(roomId, pollId);

        boolean isCreator = poll.getCreatedBy().getUserId().equals(currentUserId);
        boolean isAdmin = actor.getRoomMemberRole() == RoomRole.ADMIN;

        if(!isCreator && !isAdmin){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ensureActive(poll);   // đã đóng rồi thì báo lỗi
        poll.setIsActive(false);

        // TODO WebSocket: đẩy trạng thái đã đóng
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
    private Room getRoomOrThrow(UUID roomId){
        return roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );
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
        }

        response.setTotalVoters(pollVoteRepository.countVoters(pollId));
        return response;
    }


    //đảm bảo poll chưa bị đóng
    private void ensureActive(Poll poll){
        if(!Boolean.TRUE.equals(poll.getIsActive())){
            throw new AppException(ErrorCode.POLL_CLOSED);
        }
    }

}
