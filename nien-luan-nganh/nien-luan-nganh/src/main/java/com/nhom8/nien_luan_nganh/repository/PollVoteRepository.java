package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    // phiếu hiện tại của một người trong một poll
    List<PollVote> findByPollOption_Poll_PollIdAndUser_UserId(UUID pollId, UUID userId);

    // đếm phiếu mỗi option bằng 1 query: [optionId, count]
    @Query("""
                select v.pollOption.pollOptionId, count(v)
                from PollVote v
                where v.pollOption.poll.pollId = :pollId
                group by v.pollOption.pollOptionId
            """)
    List<Object[]> countVotesByOption(@Param("pollId") UUID pollId);

    // số người đã tham gia bình chọn (mỗi người tính 1)
    @Query("select count(distinct v.user.userId) from PollVote v where v.pollOption.poll.pollId = :pollId")
    long countVoters(@Param("pollId") UUID pollId);

    // lấy tất cả phiếu của poll kèm thông tin user để hiển thị ai đã chọn gì
    @Query("""
                select v from PollVote v
                join fetch v.user
                join fetch v.pollOption
                where v.pollOption.poll.pollId = :pollId
                order by v.votedAt asc
            """)
    List<PollVote> findAllWithUserByPollId(@Param("pollId") UUID pollId);
}
