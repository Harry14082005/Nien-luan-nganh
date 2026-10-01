package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.ReminderParticipant;
import com.nhom8.nien_luan_nganh.entity.ReminderParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ReminderParticipantRepository extends JpaRepository<ReminderParticipant, ReminderParticipantId> {

}
