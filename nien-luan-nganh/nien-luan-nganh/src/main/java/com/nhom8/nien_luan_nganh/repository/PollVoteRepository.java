package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

}
