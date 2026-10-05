package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.PollResource;
import com.nhom8.nien_luan_nganh.entity.PollResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PollResourceRepository extends JpaRepository<PollResource, PollResourceId> {

}
