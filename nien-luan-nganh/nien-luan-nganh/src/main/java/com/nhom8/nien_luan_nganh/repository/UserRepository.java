package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.UserRole;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Query("SELECT u FROM User u WHERE u.email = :q OR u.phoneNumber = :q")
    Optional<User> findByEmailOrPhoneNumber(@Param("q") String q);

    // Tim kiem nguoi dung theo email, SĐT hoac ten hien thi (loai bo tai khoan bi khoa va chinh nguoi dang tim kiem)
    @Query("SELECT u FROM User u WHERE (" +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR u.phoneNumber LIKE CONCAT('%', :query, '%') " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (u.isBanned = false OR u.isBanned IS NULL) " +
           "AND u.userId != :currentUserId")
    Page<User> searchUsers(
            @Param("query") String query,
            @Param("currentUserId") UUID currentUserId,
            Pageable pageable
    );

    // Bo loc cua Admin
    List<User> findByIsBanned(Boolean isBanned);

    List<User> findByRole(UserRole role);
}
//    // tim ban be
//    @Query("SELECT u FROM User u WHERE u.email= :q OR u.phoneNumber = :q")
//    Optional<User> findByEmailOrPhoneNumber(@Param("q") String q);
//
//    // Bo loc cua Admin
//    List<User> findByIsBanned(Boolean isBanned);
//
//    List<User> findByRole(UserRole role);
//
//    //Dung khi User co the dung sdt haoc email
//    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.phoneNumber = :identifier")
//    Optional<User> findByEmailOrPhoneNumber(@Param("identifier") String identifier);



