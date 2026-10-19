package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.MessageReaction;
import com.nhom8.nien_luan_nganh.entity.MessageReactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, MessageReactionId> {

    // Lấy danh sách cảm xúc của một tin nhắn theo messageId
    List<MessageReaction> findByMessageReactionId_MessageId(UUID messageId);

    // Đếm tổng số cảm xúc của một tin nhắn theo messageId
    long countByMessageReactionId_MessageId(UUID messageId);
}
