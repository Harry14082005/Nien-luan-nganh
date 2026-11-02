package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollRepository extends JpaRepository<Poll, UUID> {

    List<Poll> findByRoom_RoomIdOrderByCreatedAtDesc(UUID roomId);

    // Đếm số cuộc bình chọn (Poll) mới tạo chưa đọc theo từng phòng chat trong 1 query gộp
    @Query("SELECT p.room.roomId, COUNT(p) " +
            "FROM Poll p, RoomMember rm " +
            "WHERE rm.room.roomId = p.room.roomId " +
            "AND rm.roomMemberId.userId = :userId " +
            "AND p.room.roomId IN :roomIds " +
            "AND (p.createdBy IS NULL OR p.createdBy.userId <> :userId) " +
            "AND (rm.lastReadMessageId IS NULL OR p.createdAt > COALESCE(" +
            "    (SELECT mr.createdAt FROM Message mr WHERE mr.messageId = rm.lastReadMessageId)," +
            "    (SELECT pr.createdAt FROM Poll pr WHERE pr.pollId = rm.lastReadMessageId)," +
            "    (SELECT rr.createdAt FROM Reminder rr WHERE rr.reminderId = rm.lastReadMessageId)" +
            ")) " +
            "GROUP BY p.room.roomId")
    List<Object[]> countUnreadPollsByRoomIds(@Param("roomIds") List<UUID> roomIds,
            @Param("userId") UUID userId);
}
