package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poll_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResource {

    @EmbeddedId
    private PollResourceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pollId")
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

}
