package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
<<<<<<< HEAD
@Table(name = "poll_resources")
=======
@Table(name="poll_resource")
>>>>>>> fbdb5e85ab9add93784081c0956ce19e7236f639
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
