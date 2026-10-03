package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoomId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "room_id")
    private UUID roomId;
}
