package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.PollResource;
import com.nhom8.nien_luan_nganh.entity.PollResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PollResourceRepository extends JpaRepository<PollResource, PollResourceId> {

    // Tìm liên kết theo pollId
    Optional<PollResource> findByPoll_PollId(UUID pollId);

    // Lấy liên kết theo danh sách pollIds
    @Query("SELECT pr FROM PollResource pr WHERE pr.poll.pollId IN :pollIds")
    List<PollResource> findByPoll_PollIdIn(@Param("pollIds") List<UUID> pollIds);

    // Tìm liên kết theo messageId
    Optional<PollResource> findByMessage_MessageId(UUID messageId);

    // Lấy tất cả poll_resource cho danh sách messageId
    @Query("SELECT pr FROM PollResource pr WHERE pr.message.messageId IN :messageIds")
    List<PollResource> findByMessage_MessageIdIn(@Param("messageIds") List<UUID> messageIds);
}
