package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResource {
    @EmbeddedId
    private PollResourceId pollResourceId;

    @ManyToOne
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    Message message;

    @ManyToOne
    @MapsId("pollId")
    @JoinColumn(name = "poll_id")
    Poll poll;

}
