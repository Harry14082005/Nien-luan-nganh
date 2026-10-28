package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.MessageReaction;
import com.nhom8.nien_luan_nganh.entity.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, MessageReactionId> {

    // Lấy danh sách cảm xúc của một tin nhắn kèm thông tin người dùng (tránh N+1 query)
    @Query("SELECT mr FROM MessageReaction mr JOIN FETCH mr.user WHERE mr.messageReactionId.messageId = :messageId")
    List<MessageReaction> findByMessageIdWithUser(@Param("messageId") UUID messageId);

    // Đếm tổng số cảm xúc của một tin nhắn theo messageId
    long countByMessageReactionId_MessageId(UUID messageId);

    // Xóa toàn bộ cảm xúc của tin nhắn (khi thu hồi)
    // clearAutomatically dam bao Persistence Context duoc don sach, tranh khi ung dung doc lai data cu con sot trong cache
    @Modifying(clearAutomatically = true) 
    @Query("DELETE FROM MessageReaction mr WHERE mr.messageReactionId.messageId = :messageId")
    void deleteByMessageId(@Param("messageId") UUID messageId);
}
