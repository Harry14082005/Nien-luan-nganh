package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.ReminderResource;
import com.nhom8.nien_luan_nganh.entity.ReminderResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ReminderResourceRepository extends JpaRepository<ReminderResource, ReminderResourceId> {

}
