package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollResourceId implements Serializable {
    private UUID messageId;
    private UUID pollId;
}
