package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollResourceId implements Serializable {

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "poll_id")
    private String pollId;

}
