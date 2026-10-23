package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.MessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface MessageHistoryRepository extends JpaRepository<MessageHistory, UUID> {

    // Lay lich su chinh sua moi nhat
    @Query("SELECT mh FROM MessageHistory mh WHERE mh.message.messageId = :messageId ORDER BY mh.editedAt DESC")
    List<MessageHistory> findHistoryByMessageId(@Param("messageId") UUID messageId);

    // List<MessageHistory> findByMessage_MessageIdOrderByEditedAtDesc (UUID
    // messageId);

}
