package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Auth
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    // tim ban be
    @Query("SELECT u FROM User u WHERE u.email= :q OR u.phoneNumber = :q")
    Optional<User> findByEmailOrPhoneNumber(@Param("q") String q);

    // Bo loc cua Admin
    List<User> findByIsBanned(Boolean isBanned);

    List<User> findByRole(UserRole role);

}
