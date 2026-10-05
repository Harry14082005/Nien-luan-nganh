package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollResourceId implements Serializable {
<<<<<<< HEAD

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "poll_id")
    private UUID pollId;

=======
    private UUID messageId;
    private UUID pollId;
>>>>>>> fbdb5e85ab9add93784081c0956ce19e7236f639
}
