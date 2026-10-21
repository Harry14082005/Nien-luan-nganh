package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class PollVoteId implements Serializable {
    @Column(name = "pollOptionId")
    private UUID pollOptionId;

    @Column(name = "userId")
    private UUID userId;
}
